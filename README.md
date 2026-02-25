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

## Fichiers créés / modifiés

### Fichiers créés

| Fichier | Package | Rôle |
|---|---|---|
| `User.java` | `auth.domain` | Entité JPA : `email` + `verified` (false par défaut) |
| `VerificationToken.java` | `auth.domain` | Stocke `tokenId` (public) + `tokenHash` (BCrypt) + `expiresAt` |
| `UserRepository.java` | `auth.repository` | `findByEmail()` |
| `VerificationTokenRepository.java` | `auth.repository` | `findById(tokenId)` |
| `UserRegisteredEvent.java` | `auth.event` | POJO de l'événement publié dans RabbitMQ |
| `AuthService.java` | `auth.service` | Logique inscription + génération token + publication event + vérification BCrypt |
| `AuthController.java` | `auth.controller` | `POST /register` et `GET /verify` |
| `RabbitConfig.java` | `auth.config` | Déclare exchange topic + DLX + DLQ |
| `NotificationRabbitConfig.java` | `notification.config` | Déclare la file avec `x-dead-letter-exchange` |
| `EmailService.java` | `notification.service` | Construit le lien de vérification et envoie l'e-mail via MailHog |
| `UserRegisteredConsumer.java` | `notification.consumer` | `@RabbitListener` sur la file, déclenche l'envoi d'e-mail |

### Fichiers modifiés

| Fichier | Modification |
|---|---|
| `pom.xml` | Ajout des dépendances : `spring-boot-starter-amqp`, `spring-boot-starter-mail`, `jackson-datatype-jsr310`, `spring-security-crypto` |
| `application.properties` | Ajout de la config RabbitMQ, SMTP MailHog, paramètres `app.mq.*`, `app.mail.*`, `app.token.*` |

---

## Structure du projet

```
src/main/java/com/example/demo/
├── DemoApplication.java
├── auth/
│   ├── auth-archi/                        ← Fichiers Bruno (tests API)
│   │   ├── Authority/, Credential/
│   │   ├── Identity/, Token/
│   │   ├── Login.bru, Logout.bru
│   │   ├── Register.bru, Verify.bru
│   │   └── environments/local.bru
│   ├── controller/
│   │   ├── AuthController.java            ← POST /register, GET /verify  [NOUVEAU]
│   │   ├── AuthorityController.java
│   │   ├── CredentialController.java
│   │   ├── IdentityController.java
│   │   └── TokenController.java
│   ├── domain/
│   │   ├── AuthMethod.java
│   │   ├── Authority.java
│   │   ├── Credential.java
│   │   ├── Identity.java
│   │   ├── Token.java
│   │   ├── User.java                      ← Entité utilisateur  [NOUVEAU]
│   │   └── VerificationToken.java         ← Token hashé BCrypt  [NOUVEAU]
│   ├── event/
│   │   └── UserRegisteredEvent.java       ← Événement RabbitMQ  [NOUVEAU]
│   ├── repository/
│   │   ├── AuthorityRepository.java
│   │   ├── CredentialRepository.java
│   │   ├── IdentityRepository.java
│   │   ├── TokenRepository.java
│   │   ├── UserRepository.java            ← [NOUVEAU]
│   │   └── VerificationTokenRepository.java ← [NOUVEAU]
│   └── service/
│       ├── AuthorityService.java
│       ├── AuthService.java               ← Logique inscription/vérification  [NOUVEAU]
│       ├── CredentialService.java
│       ├── IdentityService.java
│       └── TokenService.java
├── config/
│   ├── H2ConsoleConfig.java
│   └── RabbitConfig.java                  ← Exchange + DLX + DLQ  [NOUVEAU]
├── notification/
│   ├── consumer/
│   │   └── UserRegisteredConsumer.java    ← @RabbitListener  [NOUVEAU]
│   └── service/
│       └── EmailService.java              ← Envoi e-mail MailHog  [NOUVEAU]
└── web/
    └── HomeController.java
```

---

## Prérequis

- Java 17+
- Maven 3.8+
- Homebrew (Mac) ou Docker

---

## Installation et démarrage

### Sur Mac (via Homebrew)

```bash
# Installer Homebrew si pas déjà fait
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Installer RabbitMQ
brew install rabbitmq
brew services start rabbitmq

# Installer et démarrer MailHog (laisser ce terminal ouvert)
brew install mailhog
mailhog
```

Vérifier dans le navigateur :
- **RabbitMQ UI** → http://localhost:15672 (guest / guest)
- **MailHog UI** → http://localhost:8025

---

### Sur Windows (via Docker Desktop)

**1. Installer Docker Desktop**

Télécharger et installer Docker Desktop : https://www.docker.com/products/docker-desktop

Vérifier l'installation dans un terminal PowerShell :
```powershell
docker --version
```

**2. Créer un fichier `docker-compose.yml`** à la racine du projet avec ce contenu :

```yaml
version: "3.9"
services:
  rabbitmq:
    image: rabbitmq:3-management
    container_name: rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  mailhog:
    image: mailhog/mailhog
    container_name: mailhog
    ports:
      - "1025:1025"
      - "8025:8025"
```

**3. Lancer RabbitMQ + MailHog**

```powershell
docker-compose up -d
```

Vérifier que les conteneurs tournent :
```powershell
docker ps
```

Vérifier dans le navigateur :
- **RabbitMQ UI** → http://localhost:15672 (guest / guest)
- **MailHog UI** → http://localhost:8025

---

### 2. Lancer le projet Spring Boot (Mac et Windows)

Dans un nouveau terminal, depuis le dossier du projet :

```bash
mvn spring-boot:run
```

L'application est prête quand tu vois :
```
Started DemoApplication in X seconds
```

---

## Test du flux complet (étape par étape)

### Étape 1 — Inscription

Dans un nouveau terminal :

```bash
curl -s -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com"}'
```

**Réponse attendue :**
```json
{
  "status": "REGISTERED",
  "message": "Un e-mail de vérification a été envoyé"
}
```

**Dans la console Spring Boot, tu dois voir :**
```
[AUTH] Utilisateur créé id=1 email=alice@example.com
[AUTH] Token créé tokenId=xxx expiresAt=...
[AUTH] Événement UserRegistered publié eventId=xxx
[NOTIFICATION] Événement reçu eventId=xxx email=alice@example.com
[NOTIFICATION] E-mail envoyé à alice@example.com tokenId=xxx
```

---

### Étape 2 — Consulter l'e-mail dans MailHog

Ouvrir **http://localhost:8025**

Tu dois voir un e-mail pour `alice@example.com` contenant un lien :
```
http://localhost:8080/verify?tokenId=XXX&t=YYY
```

---

### Étape 3 — Vérifier l'e-mail (cliquer le lien)

Copier le lien depuis MailHog et lancer :

```bash
curl -s "http://localhost:8080/verify?tokenId=XXX&t=YYY"
```

**Réponse attendue :**
```json
{
  "status": "VERIFIED",
  "message": "E-mail vérifié avec succès !"
}
```

---

### Étape 4 — Vérifier en base H2

Ouvrir **http://localhost:8080/h2-console**
- JDBC URL : `jdbc:h2:mem:demo`
- User : `sa`
- Password : *(laisser vide)*

```sql
-- L'utilisateur doit avoir VERIFIED = TRUE
SELECT * FROM users;

-- La table doit être vide (token supprimé après usage one-shot)
SELECT * FROM verification_tokens;
```

---

### Étape 5 — Tester le doublon (erreur attendue)

```bash
curl -s -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com"}'
```

**Réponse attendue :**
```json
{
  "error": "Email déjà utilisé : alice@example.com"
}
```

---

### Étape 6 — Vérifier les files dans RabbitMQ UI

Ouvrir **http://localhost:15672** → onglet **Queues**

Tu dois voir :
- `notification.user-registered` — file principale
- `notification.user-registered.dlq` — dead letter queue
- `auth.events.dlq` — dead letter queue globale

---

## Critères d'évaluation couverts

| Critère | Statut |
|---|---|
| Flux fonctionnel complet : inscription → e-mail → vérification (40%) | ✅ |
| Sécurité : token non stocké en clair, expiration respectée (25%) | ✅ |
| Messagerie : configuration correcte, DLQ opérationnelle (20%) | ✅ |
| Qualité : logs clairs, README, tests manuels reproductibles (15%) | ✅ |

---

## Points clés à retenir

- Le token n'est **jamais stocké en clair** en base — uniquement son hash BCrypt
- Le token est **à usage unique** — supprimé immédiatement après vérification
- La messagerie assure le **découplage** entre Auth et Notification
- La **DLQ** isole les messages en erreur pour éviter de bloquer la file principale
- L'`eventId` UUID permet l'**idempotence** côté consommateur

