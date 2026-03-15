# Projet Software Architecture 2026 - M1 Informatique
## TP – Vérification d'e-mail avec messagerie asynchrone
### Spring Boot + RabbitMQ + MailHog 

---
## Groupe :
- BACHA Hiba
- BENSALLAH Younes
- EL JAGHAOUI Abdelhafid

---

## Objectifs réalisés

Ce TP implémente un mini-système d'inscription avec vérification d'e-mail asynchrone, intégré dans le projet `demo` existant. Les objectifs couverts sont :

- Découplage entre services via RabbitMQ (messagerie asynchrone)
- Flux d'inscription avec envoi d'e-mail et vérification par lien
- Stockage sécurisé du token de vérification sous forme de hash BCrypt
- Configuration d'un exchange, d'une file et d'une DLQ RabbitMQ
- Test de bout en bout avec MailHog (serveur SMTP local)

---

## Architecture du flux

```
Client
  │
  └─► POST /register (Auth)
        │
        ├─► Crée User (verified=false) en base H2
        ├─► Génère token UUID → stocke BCrypt(token) en base
        └─► Publie événement "UserRegistered" sur RabbitMQ
                │
                └─► Exchange: auth.events (topic)
                      │
                      └─► Queue: notification.user-registered
                            │
                            └─► Consommateur Notification
                                  │
                                  └─► Envoie e-mail via MailHog (SMTP :1025)
                                        │
                                        └─► Lien: GET /verify?tokenId=...&t=...
                                                │
                                                └─► BCrypt.matches(t, hash)
                                                └─► User.verified = true
                                                └─► Token supprimé (one-shot)
```

---

## Pourquoi hasher le token ?

Comme pour les mots de passe, on ne stocke **jamais** un secret en clair en base de données. En cas de fuite, les tokens ne peuvent pas être exploités.

| Étape | Ce qui se passe |
|---|---|
| Inscription | `tokenClear = UUID.randomUUID()` (secret généré) |
| Stocké en base | `tokenHash = BCrypt(tokenClear)` |
| Dans l'URL du lien | `?t=tokenClear` |
| Vérification | `BCrypt.matches(tokenClear, tokenHash)` → true/false |

---

## Pourquoi RabbitMQ ?

- **Découplage** : Auth n'attend pas la fin de l'envoi d'e-mail, il publie un fait métier
- **Résilience** : si MailHog est indisponible, les messages s'accumulent dans la file et sont traités dès que possible
- **DLQ** : les messages en erreur partent en Dead Letter Queue pour analyse

---

## Structure du projet

```
Projet_Software_Architecture_2026/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── interface.html
├── README.md
├── mvnw / mvnw.cmd
│
├── nginx/
│   └── nginx.conf
│
├── service-a/
│   └── Dockerfile
│
├── service-b/
│   └── Dockerfile
│
└── src/main/
    ├── resources/
    │   ├── application.properties
    │   └── static/
    │       └── index.html
    │
    └── java/com/example/demo/
        ├── DemoApplication.java
        ├── web/
        │   └── HomeController.java
        ├── config/
        │   ├── RabbitConfig.java
        │   ├── CorsConfig.java
        │   └── H2ConsoleConfig.java
        ├── auth/
        │   ├── domain/
        │   │   ├── User.java
        │   │   ├── VerificationToken.java
        │   │   ├── Identity.java
        │   │   ├── Authority.java
        │   │   ├── Credential.java
        │   │   ├── Token.java
        │   │   └── AuthMethod.java
        │   ├── repository/
        │   │   ├── UserRepository.java
        │   │   ├── VerificationTokenRepository.java
        │   │   ├── IdentityRepository.java
        │   │   ├── AuthorityRepository.java
        │   │   ├── CredentialRepository.java
        │   │   └── TokenRepository.java
        │   ├── service/
        │   │   ├── AuthService.java
        │   │   ├── TokenStore.java
        │   │   ├── IdentityService.java
        │   │   ├── AuthorityService.java
        │   │   ├── CredentialService.java
        │   │   └── TokenService.java
        │   ├── event/
        │   │   ├── UserRegisteredEvent.java
        │   │   └── EmailVerifiedEvent.java
        │   ├── controller/
        │   │   ├── AuthController.java
        │   │   ├── UserController.java
        │   │   ├── IdentityController.java
        │   │   ├── AuthorityController.java
        │   │   ├── CredentialController.java
        │   │   └── TokenController.java
        │   └── auth-archi/
        │       ├── Login.bru, Logout.bru, Register.bru, Verify.bru
        │       ├── Authority/, Credential/, Identity/, Token/
        │       └── environments/local.bru
        └── notification/
            ├── consumer/
            │   ├── UserRegisteredConsumer.java
            │   └── AnalyticsConsumer.java
            └── service/
                └── EmailService.java
```

---

## Prérequis

- Docker Desktop (https://www.docker.com/products/docker-desktop)

---

## Installation et démarrage

```bash
docker compose up --build -d
```

Vérifier que les 7 conteneurs tournent :
```bash
docker ps
```

Interfaces web :
- **Application** → http://localhost (NGINX port 80)
- **MailHog** → http://localhost:8025
- **RabbitMQ UI** → http://localhost:15672 (guest / guest)

---

## Test du flux complet

### 1. Inscription

```bash
curl -s -X POST http://localhost/register \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com", "password": "secret123"}'
```

### 2. Vérifier l'e-mail dans MailHog

Ouvrir **http://localhost:8025** → copier le lien de vérification → l'ouvrir dans le navigateur.

### 3. Login

```bash
curl -s -X POST http://localhost/login \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com", "password": "secret123"}'
```

Retourne un `token` UUID.

### 4. Accès aux services protégés

```bash
# Avec token → 200 "hello A"
curl -s http://localhost/service-a -H "Authorization: Bearer <token>"

# Sans token → 403 Accès refusé
curl -s http://localhost/service-a
```

### 5. Vérifier les files RabbitMQ

Ouvrir **http://localhost:15672** → onglet **Queues** :
- `notification.user-registered` — file principale
- `auth.events.dlq` — dead letter queue


---

## Séance 2 — Étapes réalisées

### Étape 8 — Idempotence de la vérification

La méthode `verify()` dans `AuthService` a été rendue idempotente : si le token est introuvable (déjà consommé) ou que l'utilisateur est déjà vérifié, le système retourne `ALREADY_VERIFIED` au lieu de lever une exception.

Un enum `VerifyResult` a été introduit :

```java
public enum VerifyResult {
    SUCCESS, ALREADY_VERIFIED, EXPIRED, INVALID
}
```

Le contrôleur retourne une réponse `200 OK` avec le statut `ALREADY_VERIFIED` (pas d'erreur 4xx) pour permettre les appels répétés sans effet de bord.

**Test :**
```bash
# Premier appel → VERIFIED
curl -s "http://localhost/verify?tokenId=XXX&t=YYY"

# Deuxième appel sur le même lien → ALREADY_VERIFIED (pas d'erreur)
curl -s "http://localhost/verify?tokenId=XXX&t=YYY"
```

---

### Étape 9 — Dead Letter Queue (DLQ) et simulation d'erreur

Une propriété de simulation d'erreur a été ajoutée dans `application.properties` :

```properties
app.notification.simulate-error=false
```

Quand elle est activée (`true`), le consommateur `UserRegisteredConsumer` lève une `RuntimeException`, ce qui force le message à partir en Dead Letter Queue après épuisement des tentatives.

Les files visibles dans RabbitMQ UI (`http://localhost:15672` → Queues) :
- `notification.user-registered` — file principale
- `notification.user-registered.dlq` — messages en erreur
- `auth.events.dlq` — dead letter queue globale de l'exchange

**Pour tester la DLQ :**
1. Passer `app.notification.simulate-error=true` dans `application.properties`
2. Lancer `POST /register`
3. Observer dans RabbitMQ UI que le message arrive dans la DLQ
4. Remettre à `false` pour le fonctionnement normal

---

### Étape 10 — Analytics via événement EmailVerified

Un second événement RabbitMQ a été introduit : `EmailVerifiedEvent`, publié par `AuthService.verify()` après une vérification réussie.

**Fichiers ajoutés :**

| Fichier | Package | Rôle |
|---|---|---|
| `EmailVerifiedEvent.java` | `auth.event` | POJO de l'événement publié après vérification |
| `AnalyticsConsumer.java` | `notification.consumer` | `@RabbitListener` sur `analytics.email-verified`, compteur `AtomicInteger` |

**Flux :**
```
GET /verify (succès)
  └─► Publie EmailVerifiedEvent sur RabbitMQ
        └─► Exchange: auth.events
              └─► Queue: analytics.email-verified
                    └─► AnalyticsConsumer
                          └─► Logs: [ANALYTICS] Total emails vérifiés : N
```

**Extrait des logs attendus :**
```
[ANALYTICS] EmailVerified reçu eventId=xxx userId=1
[ANALYTICS] Total emails vérifiés : 1
```

---

## Séance 3 — NGINX Reverse Proxy + Services protégés

### Principe

NGINX sert de **point d'entrée unique** (port 80). Il joue le rôle de reverse proxy devant tous les services.

Certaines routes sont **publiques** (`/register`, `/login`, `/verify`) et sont transmises directement au service Auth (Spring Boot).

D'autres routes sont **protégées** (`/service-a`, `/service-b`). Avant de transmettre la requête, NGINX fait un **subrequest interne** (`auth_request`) vers l'endpoint `/auth/validate` du service Auth. Celui-ci vérifie le header `Authorization: Bearer <token>` :
- Si le token est valide → NGINX laisse passer vers le service cible
- Sinon → **403 Accès refusé**

Deux pseudo-services A et B (simples conteneurs NGINX) répondent `hello A` et `hello B` pour démontrer le mécanisme.

### Architecture

```
Client → NGINX (:80)
            │
            ├── /register, /login, /verify → Auth (Spring Boot)
            │
            ├── /service-a → [auth_request → Auth] → Service A ("hello A")
            └── /service-b → [auth_request → Auth] → Service B ("hello B")
```

### Conteneurs Docker (docker-compose)

| Service | Rôle | Port |
|---|---|---|
| nginx | Reverse proxy, CORS, auth_request | 80 |
| app1 / app2 | Spring Boot Auth | 8080 (interne) |
| service-a | Pseudo-service "hello A" | 80 (interne) |
| service-b | Pseudo-service "hello B" | 80 (interne) |
| rabbitmq | Messaging asynchrone | 5672 / 15672 |
| mailhog | SMTP local | 1025 / 8025 |

### Flux de test

1. `POST /register` → inscription + email de vérification
2. Vérifier l'email via MailHog → `GET /verify`
3. `POST /login` → retourne un token
4. `GET /service-a` avec `Authorization: Bearer <token>` → `hello A` (200)
5. `GET /service-a` sans token → `403 Accès refusé`

---

## Critères d'évaluation — bilan complet

| Critère | Statut |
|---|---|
| Flux fonctionnel complet : inscription → e-mail → vérification | ✅ |
| Sécurité : token hashé BCrypt, expiration | ✅ |
| Messagerie : RabbitMQ, DLQ | ✅ |
| Qualité : logs, README, tests reproductibles | ✅ |
| Idempotence de la vérification | ✅ |
| DLQ et simulation d'erreur | ✅ |
| Événement EmailVerified + Analytics | ✅ |
| NGINX reverse proxy + auth_request | ✅ |
| Services protégés A et B | ✅ |
| Docker Compose (7 conteneurs) | ✅ |