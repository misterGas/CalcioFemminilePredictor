# Calcio Femminile Predictor
Calcio Femminile Predictor è un'applicazione Android riguardante i pronostici di partite di calcio femminile. 
L'applicazione è stata sviluppata utilizzando il linguaggio Kotlin e viene fatto utilizzo della piattaforma Firebase di Google che permette l'autenticazione dell'utente (tramite Firebase Authentication) e la gestione del database (tramite Firestore).

## Funzionalità
Un utente può registrarsi nell'applicazione fornendo Nome, Cognome, Email e scegliendo una password. Inoltre, a sua scelta, decide un nickname unico all'interno dell'applicazione che sarà visualizzato nelle classifiche dei pronostici.
Ogni utente può pronosticare per ogni partita:
- il risultato (reti segnate per ogni squadra)
- le marcatrici
- l'MVP (Most Valuable Player)

e può ottenere punti in base a varie situazioni definite all'interno dell'applicazione.

Per ogni giornata di campionato, l'utente può stilare una propria Best 11: formazione formata dalle 11 giocatrici che secondo lui/lei hanno giocato meglio nei vari incontri proposti nella giornata.

Sono inoltre presenti eventi speciali di squadra: l'utente può unirsi ad una squadra insieme ai suoi amici o altri utenti della community e pronosticare partite speciali. Fase di sviluppo ancora primaria.

## Miglioramenti
Miglioramenti possibili possono essere:
- legati all'efficienza e all'efficacia dell'applicazione;
- le giocatrici di ogni squadra sono gestite all'interno dell'applicazione (a causa del necessario pagamento di un abbonamento da parte dell'API che gestisce i dati sportivi), comprese minime informazioni personali utili per la gestione dei pronostici e assegnazioni manuali delle marcatrici ufficiali tramite UI. Per questo motivo, nell'applicazione è solo presente (per ora) il campionato di Serie A Femminile. Le giocatrici possono essere aggiunte nell'applicazione, oltre tramite UI, anche creando file JSON che vengono poi elaborati per salvare le informazioni delle giocatrici nel database Firestore;
- miglioramenti al codice;
- migliore UI.

## API per i risultati delle partite
Il calendario e i risultati delle partite utilizzati nell'applicazione sono presi dal sito https://www.thesportsdb.com/free_sports_api. I dati attualmente vengono reperiti tramite la versione V1 dell'API.
Per ottenere le partite di ogni giornata si esegue una query del tipo https://www.thesportsdb.com/api/v1/json/3/eventsround.php?id=5205&r=260&s=2024-2025 (id=5205: id dell'API per il campionato di Serie A Femminile, r=260: 260 è l'ultimo round di campionato, s=2024-2025: stagione). E' necessario eseguire questa query che contiene il parametro r (round) in quanto quella che restituisce tutte le partite di campionato è limitata a 100 partite con richieste gratis effettuate con API-Key "3" (https://www.thesportsdb.com/api/v1/json/3/eventsseason.php?id=5205&s=2024-2025).

## Informativa sulla Privacy
https://sites.google.com/view/calciofemminilepredictor/home-page

## Stato della pubblicazione
Attualmente l'applicazione è in una fase di test chiusi in anteprima su Google Play Console.


