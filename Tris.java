import java.util.*;
import java.util.function.*;

List<Object> eventStore = new LinkedList<>();

enum Giocatore {Alice, Bob;}

enum Segno {
  Croce, Cerchio, Vuoto;

  @Override
  public String toString() {
    return switch (this) {
      case Croce -> "X";
      case Cerchio -> "O";
      case Vuoto -> "-";
    };
  }
}

// Eventi
record SegnoSelezionato(Giocatore giocatore, Segno segno) {}

record SegnoPosizionato(Giocatore giocatore, Segno segno, int riga, int colonna) {}

record TrisIndividuato(Giocatore giocatore) {}

// Viste
record Griglia(Segno[][] segni) {
  public Griglia() {
    this(new Segno[][] {
      {Segno.Vuoto, Segno.Vuoto, Segno.Vuoto},
      {Segno.Vuoto, Segno.Vuoto, Segno.Vuoto},
      {Segno.Vuoto, Segno.Vuoto, Segno.Vuoto}
    });
  }
}

record SegnoDelGiocatore(Giocatore giocatore, Segno segno) {}

record Turno(Giocatore giocatore) {}

record NessunVincitoreOTris(boolean getAsBoolean) implements BooleanSupplier {}

record Vincitore(Optional<Giocatore> giocatore) {}

// Delegati
void salva(SegnoSelezionato event) {
  eventStore.add(event);
  System.out.format("Segno selezionato: %s%n", event);
}

void salva(SegnoPosizionato event) {
  eventStore.add(event);
  System.out.format("Segno posizionato: %s%n", event);
}

void salva(TrisIndividuato event) {
  eventStore.add(event);
  System.out.format("Tris individuato: %s%n", event);
}

// Handler
Consumer<Object> handler = notification -> {
  switch (notification) {
    case SegnoSelezionato it -> salva(it);
    case SegnoPosizionato it -> salva(it);
    case TrisIndividuato it -> salva(it);
    default -> throw new IllegalArgumentException("Evento non riconosciuto");
  }
};

void main() {
  var scanner = new Scanner(System.in);
  // Comando
  selezionaSegnoDelGiocatore(scanner);

  while (nessunVincitoreOTris().getAsBoolean()) {
    // Metodo di utilità per la stampa di una vista
    stampaGriglia();

    // Comando manuale del player
    posizionaSegnoDelGiocatoreInTurno(scanner);

    // Comando automatico del processor
    controllaTris();

    // Vista
    vincitore()
      .giocatore
      .ifPresent(giocatore -> {
        stampaGriglia();
        System.out.format("Il vincitore è: %s%n", giocatore);
      });
  }
}

// Non è un'azione, ma è un metodo di utilità
void stampaGriglia() {
  for (var riga : griglia().segni) {
    for (var it : riga) {
      System.out.print(it == null ? " " : it);
    }
    System.out.println();
  }
}

// Azioni/Comandi
void posizionaSegnoDelGiocatoreInTurno(Scanner scanner) {
  var turno = turno();
  System.out.format("%s posiziona il segno: riga, colonna:%n", turno.giocatore());

  var rigaColonna = scanner.nextLine().split(",");
  var segnoDelGiocatore = segnoDelGiocatore(turno.giocatore);
  handler.accept(new SegnoPosizionato(turno.giocatore, segnoDelGiocatore.segno, Integer.parseInt(rigaColonna[0]), Integer.parseInt(rigaColonna[1])));
}

void selezionaSegnoDelGiocatore(Scanner scanner) {
  System.out.println("Alice seleziona il segno: 1. Croce, 2. Cerchio:");
  var segno = scanner.nextLine();

  switch (segno) {
    case "1" -> {
      handler.accept(new SegnoSelezionato(Giocatore.Alice, Segno.Croce));
      handler.accept(new SegnoSelezionato(Giocatore.Bob, Segno.Cerchio));
    }
    case "2" -> {
      handler.accept(new SegnoSelezionato(Giocatore.Alice, Segno.Cerchio));
      handler.accept(new SegnoSelezionato(Giocatore.Bob, Segno.Croce));
    }
    default -> throw new IllegalArgumentException("Scelta non valida");
  }
}

void controllaTris() {
  var griglia = griglia();

  for (Giocatore giocatore : Giocatore.values()) {
    var segnoDelGiocatore = segnoDelGiocatore(giocatore);
    if (griglia.segni[0][0] == segnoDelGiocatore.segno && griglia.segni[0][1] == segnoDelGiocatore.segno && griglia.segni[0][2] == segnoDelGiocatore.segno ||
      griglia.segni[1][0] == segnoDelGiocatore.segno && griglia.segni[1][1] == segnoDelGiocatore.segno && griglia.segni[1][2] == segnoDelGiocatore.segno ||
      griglia.segni[2][0] == segnoDelGiocatore.segno && griglia.segni[2][1] == segnoDelGiocatore.segno && griglia.segni[2][2] == segnoDelGiocatore.segno ||
      griglia.segni[0][0] == segnoDelGiocatore.segno && griglia.segni[1][0] == segnoDelGiocatore.segno && griglia.segni[2][0] == segnoDelGiocatore.segno ||
      griglia.segni[0][1] == segnoDelGiocatore.segno && griglia.segni[1][1] == segnoDelGiocatore.segno && griglia.segni[2][1] == segnoDelGiocatore.segno ||
      griglia.segni[0][2] == segnoDelGiocatore.segno && griglia.segni[1][2] == segnoDelGiocatore.segno && griglia.segni[2][2] == segnoDelGiocatore.segno ||
      griglia.segni[0][0] == segnoDelGiocatore.segno && griglia.segni[1][1] == segnoDelGiocatore.segno && griglia.segni[2][2] == segnoDelGiocatore.segno ||
      griglia.segni[0][2] == segnoDelGiocatore.segno && griglia.segni[1][1] == segnoDelGiocatore.segno && griglia.segni[2][0] == segnoDelGiocatore.segno) {
      handler.accept(new TrisIndividuato(giocatore));
    }
  }
}

// Generazione viste
SegnoDelGiocatore segnoDelGiocatore(Giocatore giocatore) {
  return eventStore.stream()
    .filter(event -> event instanceof SegnoSelezionato it && it.giocatore() == giocatore)
    .map(event -> (SegnoSelezionato) event)
    .map(event -> new SegnoDelGiocatore(event.giocatore(), event.segno()))
    .findFirst()
    .orElseThrow(() -> new IllegalStateException("Segno non selezionato"));
}

Griglia griglia() {
  return eventStore.stream()
    .filter(event -> event instanceof SegnoPosizionato)
    .map(event -> (SegnoPosizionato) event)
    .reduce(new Griglia(), (griglia, segnoPosizionato) -> {
      griglia.segni[segnoPosizionato.riga()][segnoPosizionato.colonna()] = segnoPosizionato.segno();
      return griglia;
    }, (it, _) -> it);
}

Turno turno() {
  return eventStore.reversed().stream()
    .findFirst()
    .map(event -> switch (event) {
      case SegnoPosizionato it when it.giocatore == Giocatore.Alice -> new Turno(Giocatore.Bob);
      default -> new Turno(Giocatore.Alice);
    })
    .orElseThrow(() -> new IllegalStateException("Il turno non può cominciare"));
}

NessunVincitoreOTris nessunVincitoreOTris() {
  return new NessunVincitoreOTris(
    eventStore.stream().noneMatch(event -> event instanceof TrisIndividuato) &&
      eventStore.stream().filter(event -> event instanceof SegnoPosizionato).count() <= 9
  );
}

Vincitore vincitore() {
  return new Vincitore(eventStore.stream()
    .filter(event -> event instanceof TrisIndividuato)
    .map(event -> (TrisIndividuato) event)
    .map(TrisIndividuato::giocatore)
    .findFirst());
}
