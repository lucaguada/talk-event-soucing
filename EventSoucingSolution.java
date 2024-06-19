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

void controllaTris() {
  var griglia = griglia();
  var segnoDelGiocatore = segnoDelGiocatore(Giocatore.Alice);
  var segno = segnoDelGiocatore.segno;
  if (griglia.segni[0][0] == segno && griglia.segni[0][1] == segno && griglia.segni[0][2] == segno ||
      griglia.segni[1][0] == segno && griglia.segni[1][1] == segno && griglia.segni[1][2] == segno ||
      griglia.segni[2][0] == segno && griglia.segni[2][1] == segno && griglia.segni[2][2] == segno ||
      griglia.segni[0][0] == segno && griglia.segni[1][0] == segno && griglia.segni[2][0] == segno ||
      griglia.segni[0][1] == segno && griglia.segni[1][1] == segno && griglia.segni[2][1] == segno ||
      griglia.segni[0][2] == segno && griglia.segni[1][2] == segno && griglia.segni[2][2] == segno ||
      griglia.segni[0][0] == segno && griglia.segni[1][1] == segno && griglia.segni[2][2] == segno ||
      griglia.segni[0][2] == segno && griglia.segni[1][1] == segno && griglia.segni[2][0] == segno) {
    handler.accept(new TrisIndividuato(Giocatore.Alice));
  }

  segnoDelGiocatore = segnoDelGiocatore(Giocatore.Bob);
  segno = segnoDelGiocatore.segno;
  if (griglia.segni[0][0] == segno && griglia.segni[0][1] == segno && griglia.segni[0][2] == segno ||
      griglia.segni[1][0] == segno && griglia.segni[1][1] == segno && griglia.segni[1][2] == segno ||
      griglia.segni[2][0] == segno && griglia.segni[2][1] == segno && griglia.segni[2][2] == segno ||
      griglia.segni[0][0] == segno && griglia.segni[1][0] == segno && griglia.segni[2][0] == segno ||
      griglia.segni[0][1] == segno && griglia.segni[1][1] == segno && griglia.segni[2][1] == segno ||
      griglia.segni[0][2] == segno && griglia.segni[1][2] == segno && griglia.segni[2][2] == segno ||
      griglia.segni[0][0] == segno && griglia.segni[1][1] == segno && griglia.segni[2][2] == segno ||
      griglia.segni[0][2] == segno && griglia.segni[1][1] == segno && griglia.segni[2][0] == segno) {
    handler.accept(new TrisIndividuato(Giocatore.Bob));
  }
}

void main() {
  var scanner = new Scanner(System.in);

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

  while (eventStore.stream().noneMatch(event -> event instanceof TrisIndividuato) && eventStore.size() < 9) {
    var turno = turno();

    var griglia = griglia();
    for (var riga : griglia.segni) {
      for (var it : riga) {
        System.out.print(it == null ? " " : it);
      }
      System.out.println();
    }

    System.out.format("%s posiziona il segno: riga, colonna:%n", turno.giocatore());

    var rigaColonna = scanner.nextLine().split(",");
    var segnoDelGiocatore = segnoDelGiocatore(turno.giocatore);
    handler.accept(new SegnoPosizionato(turno.giocatore, segnoDelGiocatore.segno, Integer.parseInt(rigaColonna[0]), Integer.parseInt(rigaColonna[1])));

    controllaTris();
  }
}

