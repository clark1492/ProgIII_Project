# clientmail

Modulo client dell'applicazione di posta elettronica sviluppata per il corso di **Programmazione III** (UniTO, 2021/22).

---

## Funzione

Il client è l'interfaccia con cui l'utente interagisce con il servizio di posta. Si occupa di:

- Permettere la registrazione e il login di un utente
- Visualizzare la lista aggiornata delle email nelle cartelle (inbox, sent, bin)
- Comporre e inviare email a uno o più destinatari
- Rispondere a un'email (reply e reply-all) e inoltrarla (forward)
- Spostare le email nel cestino e eliminarle definitivamente
- Notificare l'utente con un popup quando arriva una nuova email
- Segnalare visivamente quando il server non è raggiungibile e riconnettersi automaticamente

Ogni operazione di rete avviene in un thread separato per non bloccare l'interfaccia grafica.

---

## Struttura del progetto

```
clientmail/
├── src/main/java/it/unito/clientmail/
│   ├── application/
│   │   └── Login.java               # Entry point — avvia la finestra di login
│   ├── controller/
│   │   ├── LoginController.java     # Controller della schermata di login/registrazione
│   │   └── ClientController.java    # Controller della schermata principale
│   └── model/
│       ├── LoginModel.java          # Logica login e registrazione
│       ├── ClientModel.java         # Logica client — comandi verso il server
│       └── Connection.java          # Gestione socket e stream di serializzazione
├── src/main/resources/it/unito/clientmail/
│   ├── application/
│   │   ├── login.fxml               # UI schermata login/registrazione
│   │   └── client.fxml              # UI schermata principale
│   └── img/                         # Icone e immagini
└── pom.xml
```

---

## Requisiti

- Java 17+
- Maven 3.6+
- JavaFX 17
- Il modulo `servermail` installato nel repository Maven locale

---

## Avvio

Prima di avviare il client è necessario installare il modulo servermail:

```bash
cd servermail
mvn install
```

Poi, in un terminale separato con il server già in esecuzione:

```bash
cd clientmail
mvn javafx:run
```

È possibile avviare più istanze del client contemporaneamente per simulare utenti diversi.

---

## Funzionalità dell'interfaccia

### Schermata di login
- **Log In** — autenticazione con email e password
- **New User** — registrazione di un nuovo account
- Gestione automatica del server non raggiungibile con pulsante Retry

### Schermata principale

| Funzione | Descrizione |
|---|---|
| **New Mail** | Apre il pannello di composizione email |
| **Inbox** | Mostra le email ricevute |
| **Sent** | Mostra le email inviate |
| **Bin** | Mostra le email nel cestino |
| **Reply** | Risponde al mittente |
| **Reply All** | Risponde al mittente e a tutti i destinatari |
| **Forward** | Inoltra l'email a nuovi destinatari |
| **Delete** | Sposta l'email nel cestino (o la elimina se già nel cestino) |
| **Send** | Invia l'email composta |
| **Undo** | Cancella i campi di composizione |
| **Logout** | Disconnette l'utente con conferma |

---

## Architettura

Il client segue il pattern **MVC** con Observer/Observable tramite le properties di JavaFX:

- `Login` è l'entry point JavaFX. Mantiene un riferimento allo `Stage` principale accessibile globalmente.
- `LoginController` gestisce la schermata di login. Ogni operazione di rete parte in un thread separato; i campi UI vengono letti prima di entrare nel thread e gli aggiornamenti avvengono via `Platform.runLater`.
- `LoginModel` comunica col server per login e registrazione. Tutti gli aggiornamenti della UI (messaggi di errore, apertura della scena client) avvengono via `Platform.runLater`.
- `ClientController` gestisce la schermata principale tramite thread interni: `Update` fa polling ogni 3 secondi per le nuove email, `Refresh` carica le cartelle, `SendThread` invia, `DeleteThread` elimina, `NotifyThread` mostra il popup di notifica. In caso di errore di connessione, `Reconnect` ritenta automaticamente ogni 3 secondi.
- `ClientModel` espone le property JavaFX (`emailListProperty`, `currentMailProperty`) a cui il controller si aggancia tramite binding e listener, senza comunicazione diretta tra vista e model.
- `Connection` incapsula la socket e i relativi stream. Tutti gli accessi sono sincronizzati con `synchronized(connection)` per evitare scritture concorrenti.

---

## Validazione input

Il client valida gli input prima di contattare il server:

- Oggetto e destinatari non possono essere vuoti
- Ogni indirizzo email viene validato con regex prima dell'invio
- Non è possibile inviare un'email a se stessi
- I destinatari multipli si separano con spazio, virgola o punto e virgola

---

## Note

- Il client si connette sempre a `127.0.0.1:8189` — le costanti sono in `Connection.java`
- Se il server viene spento, il client mostra l'icona di errore e si riconnette automaticamente quando il server torna online
