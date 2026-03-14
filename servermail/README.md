# servermail

Modulo server dell'applicazione di posta elettronica sviluppata per il corso di **Programmazione III** (UniTO, 2021/22).

---

## Funzione

Il server è il cuore dell'applicazione. Si occupa di:

- Accettare le connessioni dei client sulla porta **8189**
- Gestire la registrazione e l'autenticazione degli utenti
- Ricevere le email inviate dai client e recapitarle nelle caselle dei destinatari
- Rispondere alle richieste di lettura delle cartelle (inbox, sent, bin)
- Gestire lo spostamento e l'eliminazione delle email
- Segnalare le email non lette quando un client fa polling
- Mostrare in tempo reale il log di tutte le operazioni tramite GUI

Ogni client connesso viene gestito da un thread dedicato (`HandleClient`), permettendo a più utenti di operare contemporaneamente senza bloccarsi a vicenda.

---

## Struttura del progetto

```
servermail/
├── src/main/java/it/unito/servermail/
│   ├── application/
│   │   └── MainServer.java          # Entry point — avvia la GUI JavaFX
│   ├── controller/
│   │   └── ServerController.java    # Controller JavaFX — osserva il log
│   ├── handler/
│   │   └── HandleClient.java        # Thread dedicato per ogni client connesso
│   ├── model/
│   │   ├── ServerModel.java         # Logica server — accetta connessioni in loop
│   │   ├── Email.java               # Modello email (Serializable)
│   │   ├── Folder.java              # Enum cartelle: INBOX, SENT, BIN, WRITE
│   │   └── User.java                # Modello utente (Serializable)
│   └── utils/
│       └── FilesManager.java        # Persistenza su file con accessi sincronizzati
├── data/                            # Creata automaticamente al primo avvio
│   ├── server/
│   │   ├── users.dat                # Lista utenti registrati
│   │   └── id_counter.dat           # Contatore globale ID email
│   └── <email_utente>/
│       ├── inbox.dat
│       ├── sent.dat
│       └── bin.dat
└── pom.xml
```

---

## Requisiti

- Java 17+
- Maven 3.6+
- JavaFX 17

---

## Avvio

```bash
cd servermail
mvn javafx:run
```

Il server deve essere avviato **prima** dei client. Al primo avvio crea automaticamente la cartella `data/` con tutta la struttura necessaria. La porta **8189** deve essere libera.

---

## Protocollo di comunicazione

La comunicazione avviene tramite **Socket Java** con serializzazione degli oggetti (`ObjectInputStream` / `ObjectOutputStream`). I comandi sono stringhe testuali:

| Comando | Descrizione | Risposta |
|---|---|---|
| `LOGIN_REQUEST` | Autenticazione utente | `SERVER_SUCCESS` / `USER_NOT_EXIST` / `INCORRECT_PASSWORD` |
| `SIGNIN_REQUEST` | Registrazione nuovo utente | `SERVER_SUCCESS` / `USER_ALREADY_REGISTERED` / `SERVER_UNSUCCESS` |
| `MAILBOX_REQUEST` | Contenuto di una cartella | Lista `LinkedList<Email>` |
| `SEND_EMAIL` | Invio di una email | `SERVER_SUCCESS` / `USER_NOT_EXIST` |
| `DEL_EMAIL` | Eliminazione email | `SERVER_SUCCESS` / `MOVED_BIN` |
| `READ_FLAG` | Segna email come letta | `SERVER_SUCCESS` |
| `KEEP_UPDATE` | Polling nuove email non lette | Lista `LinkedList<Email>` |
| `LOG_OUT` | Disconnessione client | — |

---

## Architettura

Il server segue il pattern **MVC** con Observer/Observable tramite le properties di JavaFX:

- `ServerModel` implementa `Runnable` e gira su un thread daemon. Accetta le connessioni in loop e per ognuna crea un thread `HandleClient` separato.
- `HandleClient` gestisce l'intero ciclo di vita di un client connesso: login, comandi, disconnessione.
- `ServerController` osserva `logTextProperty()` di `ServerModel` tramite un listener JavaFX e aggiorna la `TextArea` senza accoppiamento diretto col model.
- `FilesManager` sincronizza gli accessi ai file con `synchronized` su oggetti `File` per garantire la thread safety con client multipli.

---

## Persistenza

I dati vengono salvati in file binari (`.dat`) serializzati nella cartella `data/`. La struttura viene creata automaticamente e **non deve essere committata su git** (è nel `.gitignore`).

Ogni utente ha tre file: `inbox.dat`, `sent.dat`, `bin.dat`. Gli utenti e il contatore ID globale sono salvati in `data/server/`.

---

## Note

- Le password sono salvate in chiaro — progetto accademico
- La porta `8189` è configurata come costante in `Connection.java` (lato client) e in `ServerModel.java` (lato server)
