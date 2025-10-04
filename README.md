# Calcio Femminile Predictor
Calcio Femminile Predictor è un'applicazione Android riguardante i pronostici di partite di calcio femminile. 
L'applicazione è stata sviluppata utilizzando il linguaggio Kotlin e viene fatto utilizzo della piattaforma Firebase di Google che permette l'autenticazione dell'utente (tramite Firebase Authentication) e la gestione del database (tramite Firestore).

## Funzionalità
Un utente può registrarsi nell'applicazione fornendo nome, cognome, e-mail e scegliendo una password. Inoltre, a sua scelta, decide un nickname unico all'interno dell'applicazione che sarà visualizzato nelle classifiche dei pronostici.

### Pronostico partite
Ogni utente può pronosticare per ogni partita:
- il risultato (reti segnate per ogni squadra)
- le marcatrici
- i cartellini disciplinari (ammonizioni ed espulsioni): per ogni squadra coinvolta nell'incontro saraà possibile assegnare fino ad un massimo di 3 slot per le ammonizioni e 1 slot per le espulsioni
- l'MVP (Most Valuable Player)

e può ottenere punti in base a varie situazioni definite all'interno dell'applicazione.

### Best 11
Per ogni giornata di campionato, l'utente può creare una propria Best 11: formazione formata dalle 11 giocatrici che secondo lui/lei hanno giocato meglio nei vari incontri proposti nella giornata.
Per ogni Best 11, sarà possibile assegnare un capitano tra le giocatrici attualmente aggiunte alla formazione.

Al termine della giornata di campionato verrà stilata una classifica con tutti i punti ottenuti da ciascun utente che ha creato la Best 11 e sarà possibile confrontarsi con la Best 11 creata dall'utente selezionato nella classifica.

### Eventi speciali
Sono inoltre presenti eventi speciali di squadra: l'utente può unirsi ad una squadra insieme ai suoi amici o altri utenti della community e pronosticare partite speciali. Fase di sviluppo ancora primaria.

### Alcune immagini delle funzionalità
<img src="https://github.com/user-attachments/assets/2b12023a-5cc4-4e55-bac1-36e04fff9e5e" width="200" height="500"/>
<img src="https://github.com/user-attachments/assets/31f48d65-2d9d-4a9b-bf05-2ecb95f45dd8" width="200" height="500"/>

## Miglioramenti
Miglioramenti possibili possono essere:
- legati all'efficienza e all'efficacia dell'applicazione;
- le giocatrici di ogni squadra sono gestite all'interno dell'applicazione (a causa del necessario pagamento di un abbonamento da parte dell'API che gestisce i dati sportivi), comprese minime informazioni personali utili per la gestione dei pronostici e assegnazioni manuali delle marcatrici ufficiali tramite UI. Per questo motivo, nell'applicazione è solo presente (per ora) il campionato di Serie A Femminile. Le giocatrici possono essere aggiunte nell'applicazione, oltre tramite UI, anche creando file JSON che vengono poi elaborati per salvare le informazioni delle giocatrici nel database Firestore;
- miglioramenti al codice;
- migliore UI.

## API per i risultati delle partite
Il calendario e i risultati delle partite utilizzati nell'applicazione sono presi dal sito https://www.thesportsdb.com/free_sports_api. I dati attualmente vengono reperiti tramite la versione V1 dell'API.
Per ottenere le partite di ogni giornata si esegue una query del tipo https://www.thesportsdb.com/api/v1/json/3/eventsround.php?id=5205&r=1&s=2025-2026 (id=5205: id dell'API per il campionato di Serie A Women, r=1: 1 è il primo round di stagione regolate del campionato, s=2024-2025: stagione). E' necessario eseguire questa query (eventsround.php) che contiene il parametro r (round) in quanto quella che restituisce tutte le partite di campionato è limitata a 100 partite con richieste gratis effettuate con API-Key "3" (https://www.thesportsdb.com/api/v1/json/3/eventsseason.php?id=5205&s=2025-2026).

E' inoltre prevista l'aggiunta di un campionato, la stagione e le partite associate direttamente da database, ma i risultati ufficiali di ogni incontro ad ora devono essere fatti direttamente da database. In futuro, si può prevedere l'automazione di tali attività direttamente tramite interfaccia grafica.

## Informativa sulla Privacy
https://sites.google.com/view/calciofemminilepredictor/home-page

## Stato della pubblicazione
Attualmente l'applicazione è in una fase di test chiusi in anteprima su Google Play Console. Per poter provare l'applicazione in anteprima, scrivere un'e-mail a antonio.gastaldi02@gmail.com specificando l'indirizzo di posta elettronica con cui si vuole testare il prodotto.


Creata da Antonio Gastaldi, Ingegnere Informatico laureato all'Università degli Studi di Padova. Software Developer presso D4I SRL
