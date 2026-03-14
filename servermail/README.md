# servermail

Modulo server dell'applicazione di posta elettronica sviluppata per il corso di **Programmazione III** (UniTO, 2021/22).

## Descrizione

Il server gestisce le caselle di posta elettronica degli utenti e la comunicazione con i client. Espone una GUI con il log in tempo reale di tutte le operazioni effettuate dai client connessi.

## Struttura del progetto

```
servermail/
├── src/main/java/it/unito/servermail/
│   ├── application/
│   │   └── MainServer.java          # Entry point, avvia la GUI JavaFX
│   ├── controller/
│   │   └── ServerController.java    # Controller JavaFX, osserva il log
│   ├── handler/
│   │   └── HandleClient.java        # Thread per gestire ogni client connesso
│   ├── model/
│   │   ├── ServerModel.java         # Logica server, accetta connessioni
│   │   ├── Email.java               # Modello email (Serializable)
│   │   ├── Folder.java              # Enum cartelle (INBOX, SENT, BIN, WRITE)
│   │   └── User.java                # Modello utente (Serializable)
│   └── utils/
│       └── FilesManager.java        # Gestione persistenza su file
├── data/                            # Generata automaticamente al primo avvio
│   ├── server/
│   │   ├── users.dat                # Lista utenti registrati
│   │   └── id_counter.dat          # Contatore ID email
│   └── <email_utente>/
│       ├── inbox.dat
│       ├── sent.dat
│       └── bin.dat
└── pom.xml
```

## Requisiti

- Java 17+
- Maven 3.6+
- JavaFX 17

## Avvio

```bash
cd servermail
mvn javafx:run
```

Il server rimane in ascolto sulla porta **8189**. Avviarlo **sempre prima** dei client.

Al primo avvio viene creata automaticamente la cartella `data/` con tutta la struttura necessaria.

## Protocollo di comunicazione

Il server comunica con i client tramite **Socket Java** con serializzazione degli oggetti (`ObjectInputStream` / `ObjectOutputStream`). I comandi sono stringhe testuali:

| Comando | Descrizione |
|---|---|
| `LOGIN_REQUEST` | Autenticazione utente |
| `SIGNIN_REQUEST` | Registrazione nuovo utente |
| `MAILBOX_REQUEST` | Richiesta contenuto cartella |
| `SEND_EMAIL` | Invio email |
| `DEL_EMAIL` | Eliminazione email (sposta nel cestino o rimuove) |
| `READ_FLAG` | Segna email come letta |
| `KEEP_UPDATE` | Polling nuove email non lette |
| `LOG_OUT` | Disconnessione client |

## Persistenza

I dati vengono salvati in file binari (`.dat`) nella cartella `data/`. La cartella viene creata automaticamente e **non deve essere committata su git**.

Ogni utente ha tre file:
- `inbox.dat` — posta in arrivo
- `sent.dat` — posta inviata
- `bin.dat` — cestino

## Architettura

Il server segue il pattern **MVC** con Observer/Observable tramite le properties di JavaFX:

- `ServerModel` implementa `Runnable` e gira su un thread dedicato in attesa di connessioni
- Per ogni client connesso viene creato un thread `HandleClient` separato
- `ServerController` osserva `logTextProperty()` di `ServerModel` e aggiorna la GUI senza accoppiamento diretto
- `FilesManager` sincronizza gli accessi ai file con `synchronized` per garantire la thread safety

## Note

- La porta **8189** deve essere libera prima dell'avvio
- Le password degli utenti sono salvate in chiaro — uso accademico only
