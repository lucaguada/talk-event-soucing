import java.util.*;
import java.util.function.*;

List<Object> eventStore = new LinkedList<>();

enum Player {Alice, Bob;}

enum Symbol {
  Cross, Circle, Empty;

  @Override
  public String toString() {
    return switch (this) {
      case Cross -> "X";
      case Circle -> "O";
      case Empty -> "-";
    };
  }
}

// Eventi
record SymbolSelected(Player player, Symbol symbol) {}

record SymbolPlaced(Player player, Symbol symbol, int row, int col) {}

record TicTacToeFound(Player player) {}

// Viste
record Grid(Symbol[][] symbols) {
  public Grid() {
    this(new Symbol[][] {
      {Symbol.Empty, Symbol.Empty, Symbol.Empty},
      {Symbol.Empty, Symbol.Empty, Symbol.Empty},
      {Symbol.Empty, Symbol.Empty, Symbol.Empty}
    });
  }
}

record PlayerSymbol(Player player, Symbol symbol) {}

record Turn(Player player) {}

record NoWinnerOrTicTacToe(boolean getAsBoolean) implements BooleanSupplier {}

record Winner(Optional<Player> player) {}

// Delegates
void store(SymbolSelected event) {
  eventStore.add(event);
  System.out.format("Symbol has been selected: %s%n", event);
}

void store(SymbolPlaced event) {
  eventStore.add(event);
  System.out.format("Symbol has been placed: %s%n", event);
}

void store(TicTacToeFound event) {
  eventStore.add(event);
  System.out.format("TicTacToe has been found: %s%n", event);
}

// Handler
Consumer<Object> handler = notification -> {
  switch (notification) {
    case SymbolSelected it -> store(it);
    case SymbolPlaced it -> store(it);
    case TicTacToeFound it -> store(it);
    default -> throw new IllegalArgumentException("Event not recognized");
  }
};

void main() {
  var scanner = new Scanner(System.in);
  // Command
  selectPlayerSymbol(scanner);

  while (noWinnerOrTicTacTow().getAsBoolean()) {
    // Utility method
    printGrid();

    // Manual command
    placeSymbolOfPlayer(scanner);

    // Automatic command
    checkTicTacTow();

    // View
    winner()
      .player
      .ifPresent(player -> {
        printGrid();
        System.out.format("The winner is: %s%n", player);
      });
  }
}

// It's not an action but a utility method
void printGrid() {
  for (var riga : griglia().symbols) {
    for (var it : riga) {
      System.out.print(it == null ? " " : it);
    }
    System.out.println();
  }
}

// Actions/Commands
void placeSymbolOfPlayer(Scanner scanner) {
  var turn = turn();
  System.out.format("%s place your symbol: row,col:%n", turn.player());

  var rowCol = scanner.nextLine().split(",");
  var playerSymbol = playerSymbol(turn.player);
  handler.accept(new SymbolPlaced(turn.player, playerSymbol.symbol, Integer.parseInt(rowCol[0]), Integer.parseInt(rowCol[1])));
}

void selectPlayerSymbol(Scanner scanner) {
  System.out.println("Alice select symbol: 1. Cross, 2. Circle:");
  var symbol = scanner.nextLine();

  switch (symbol) {
    case "1" -> {
      handler.accept(new SymbolSelected(Player.Alice, Symbol.Cross));
      handler.accept(new SymbolSelected(Player.Bob, Symbol.Circle));
    }
    case "2" -> {
      handler.accept(new SymbolSelected(Player.Alice, Symbol.Circle));
      handler.accept(new SymbolSelected(Player.Bob, Symbol.Cross));
    }
    default -> throw new IllegalArgumentException("Not valid symbol");
  }
}

void checkTicTacTow() {
  var griglia = griglia();

  for (Player player : Player.values()) {
    var segnoDelGiocatore = playerSymbol(player);
    if (griglia.symbols[0][0] == segnoDelGiocatore.symbol && griglia.symbols[0][1] == segnoDelGiocatore.symbol && griglia.symbols[0][2] == segnoDelGiocatore.symbol ||
      griglia.symbols[1][0] == segnoDelGiocatore.symbol && griglia.symbols[1][1] == segnoDelGiocatore.symbol && griglia.symbols[1][2] == segnoDelGiocatore.symbol ||
      griglia.symbols[2][0] == segnoDelGiocatore.symbol && griglia.symbols[2][1] == segnoDelGiocatore.symbol && griglia.symbols[2][2] == segnoDelGiocatore.symbol ||
      griglia.symbols[0][0] == segnoDelGiocatore.symbol && griglia.symbols[1][0] == segnoDelGiocatore.symbol && griglia.symbols[2][0] == segnoDelGiocatore.symbol ||
      griglia.symbols[0][1] == segnoDelGiocatore.symbol && griglia.symbols[1][1] == segnoDelGiocatore.symbol && griglia.symbols[2][1] == segnoDelGiocatore.symbol ||
      griglia.symbols[0][2] == segnoDelGiocatore.symbol && griglia.symbols[1][2] == segnoDelGiocatore.symbol && griglia.symbols[2][2] == segnoDelGiocatore.symbol ||
      griglia.symbols[0][0] == segnoDelGiocatore.symbol && griglia.symbols[1][1] == segnoDelGiocatore.symbol && griglia.symbols[2][2] == segnoDelGiocatore.symbol ||
      griglia.symbols[0][2] == segnoDelGiocatore.symbol && griglia.symbols[1][1] == segnoDelGiocatore.symbol && griglia.symbols[2][0] == segnoDelGiocatore.symbol) {
      handler.accept(new TicTacToeFound(player));
    }
  }
}

// Generazione viste
PlayerSymbol playerSymbol(Player player) {
  return eventStore.stream()
    .filter(event -> event instanceof SymbolSelected it && it.player() == player)
    .map(event -> (SymbolSelected) event)
    .map(event -> new PlayerSymbol(event.player, event.symbol))
    .findFirst()
    .orElseThrow(() -> new IllegalStateException("Segno non selezionato"));
}

Grid griglia() {
  return eventStore.stream()
    .filter(event -> event instanceof SymbolPlaced)
    .map(event -> (SymbolPlaced) event)
    .reduce(new Grid(), (grid, segnoPosizionato) -> {
      grid.symbols[segnoPosizionato.row()][segnoPosizionato.col()] = segnoPosizionato.symbol();
      return grid;
    }, (it, _) -> it);
}

Turn turn() {
  return eventStore.reversed().stream()
    .findFirst()
    .map(event -> switch (event) {
      case SymbolPlaced it when it.player == Player.Alice -> new Turn(Player.Bob);
      default -> new Turn(Player.Alice);
    })
    .orElseThrow(() -> new IllegalStateException("Can't start the game without players"));
}

NoWinnerOrTicTacToe noWinnerOrTicTacTow() {
  return new NoWinnerOrTicTacToe(
    eventStore.stream().noneMatch(event -> event instanceof TicTacToeFound) &&
      eventStore.stream().filter(event -> event instanceof SymbolPlaced).count() <= 9
  );
}

Winner winner() {
  return new Winner(eventStore.stream()
    .filter(event -> event instanceof TicTacToeFound)
    .map(event -> (TicTacToeFound) event)
    .map(TicTacToeFound::player)
    .findFirst());
}
