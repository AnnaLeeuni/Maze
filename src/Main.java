import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javalib.impworld.World;
import javalib.impworld.WorldScene;
import javalib.worldimages.FontStyle;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.TextImage;
import javalib.worldimages.WorldImage;
import tester.Tester;

// represent mazeWorld class
class MazeWorld extends World {
  public static void main(String[] args) {
    int mazeWidth = 30;
    int mazeHeight = 30;
    MazeWorld world = new MazeWorld(mazeWidth, mazeHeight);
    world.bigBang(mazeWidth * Cell.CELL_SIZE, mazeHeight * Cell.CELL_SIZE, 0.001);
  }

  int width;
  int height;
  ArrayList<Edge> edges;
  ArrayList<ArrayList<Cell>> cells;
  boolean[][] visited;
  boolean[][] attempted;
  Deque<Cell> solution;
  boolean isSolved;
  boolean useDFS;
  String status;

  // constructor
  MazeWorld(int width, int height) {
    this.width = width;
    this.height = height;
    this.edges = new ArrayList<>();
    this.cells = new ArrayList<>();
    this.visited = new boolean[height][width];
    this.solution = new LinkedList<>();
    this.isSolved = false;
    this.useDFS = true;
    this.attempted = new boolean[height][width];
    generateMaze();
    this.status = "Press D for DFS, B for BFS, N for a new maze, or S to solve.";
  }

  // EFFECT: draw the maze
  void generateMaze() {
    for (int i = 0; i < height; i++) {
      cells.add(new ArrayList<>());
      for (int j = 0; j < width; j++) {
        cells.get(i).add(new Cell(j, i));
      }
    }

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        if (j + 1 < width) {
          edges.add(new Edge(cells.get(i).get(j), cells.get(i).get(j + 1)));
        }
        if (i + 1 < height) {
          edges.add(new Edge(cells.get(i).get(j), cells.get(i + 1).get(j)));
        }
      }
    }

    Collections.shuffle(edges);
    kruskal();
  }

  // EFFEC: kruskal algo
  void kruskal() {
    for (Edge edge : edges) {
      Cell c1 = edge.c1.find();
      Cell c2 = edge.c2.find();

      if (c1 != c2) {
        c1.union(c2);
        edge.removed = true;
      }
    }
  }

  // check if the two cell is connected
  boolean isConnected(Cell c1, Cell c2) {
    for (Edge edge : edges) {
      if (edge.removed && ((edge.c1 == c1 && edge.c2 == c2) || (edge.c1 == c2 && edge.c2 == c1))) {
        return true;
      }
    }
    return false;
  }

  // EFFECT: solve the maze in DFS
  void solveDFS(Cell cell) {
    if (isSolved) {
      return;
    }
    visited[cell.y][cell.x] = true;
    solution.addFirst(cell);

    if (cell.x == width - 1 && cell.y == height - 1) {
      isSolved = true;
      return;
    }

    for (int[] dir : new int[][] { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } }) {
      int newX = cell.x + dir[0];
      int newY = cell.y + dir[1];
      if (newX >= 0 && newX < width && newY >= 0 && newY < height && !visited[newY][newX]
              && isConnected(cell, cells.get(newY).get(newX))) {
        solveDFS(cells.get(newY).get(newX));
        attempted[newY][newX] = true;
      }
    }

    if (!isSolved) {
      solution.removeFirst();
    }
  }

  // EFFECT: solve the maze in BFS
  void solveBFS() {
    Queue<Cell> queue = new LinkedList<>();
    Map<Cell, Cell> prev = new HashMap<>();

    Cell start = cells.get(0).get(0);
    queue.add(start);
    visited[start.y][start.x] = true;

    while (!queue.isEmpty()) {
      Cell current = queue.poll();

      if (current.x == width - 1 && current.y == height - 1) {
        while (current != null) {
          solution.addFirst(current);
          current = prev.get(current);
        }
        isSolved = true;
        break;
      }

      for (int[] dir : new int[][] { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } }) {
        int newX = current.x + dir[0];
        int newY = current.y + dir[1];
        if (newX >= 0 && newX < width && newY >= 0 && newY < height && !visited[newY][newX]
                && isConnected(current, cells.get(newY).get(newX))) {
          Cell neighbor = cells.get(newY).get(newX);
          visited[neighbor.y][neighbor.x] = true;
          attempted[neighbor.y][neighbor.x] = true;
          prev.put(neighbor, current);
          queue.add(neighbor);
        }
      }
    }
  }

  @Override
  // EFFECT: update the world with key pressed
  // with specific command
  public void onKeyEvent(String key) {
    if (key.equals("n") || key.equals("s")) {
      for (boolean[] row : attempted) {
        Arrays.fill(row, false);
        this.solution.clear();
      }
    }
    if (key.equals("n")) {
      this.cells.clear();
      this.edges.clear();
      this.visited = new boolean[height][width];
      this.solution.clear();
      this.isSolved = false;
      generateMaze();
      this.status = "New maze generated. Press S to solve.";
    }
    else if (key.equals("s")) {
      if (!isSolved) {
        this.status = "Solving...";
        for (boolean[] row : visited) {
          Arrays.fill(row, false);
        }
        solution.clear();
        if (useDFS) {
          solveDFS(cells.get(0).get(0));
        }
        else {
          solveBFS();
        }
        this.status = "Maze solved. Press N for a new maze.";
      }
    }
    else if (key.equals("d")) {
      useDFS = true;
      this.status = "DFS selected. Press S to solve or N for a new maze.";
    }
    else if (key.equals("b")) {
      useDFS = false;
      this.status = "BFS selected. Press S to solve or N for a new maze.";
    }
  }

  // visualize the world drawn
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(width * Cell.CELL_SIZE, height * Cell.CELL_SIZE);

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        Cell cell = cells.get(i).get(j);
        scene.placeImageXY(cell.draw(), cell.x * Cell.CELL_SIZE + Cell.CELL_SIZE / 2,
                cell.y * Cell.CELL_SIZE + Cell.CELL_SIZE / 2);
      }
    }

    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        if (attempted[i][j]) {
          WorldImage attemptedCellImage = new RectangleImage(Cell.CELL_SIZE, Cell.CELL_SIZE,
                  OutlineMode.SOLID, Color.CYAN);
          scene.placeImageXY(attemptedCellImage, j * Cell.CELL_SIZE + Cell.CELL_SIZE / 2,
                  i * Cell.CELL_SIZE + Cell.CELL_SIZE / 2);
        }
      }
    }

    for (Edge edge : edges) {
      if (!edge.removed) {
        int x1 = edge.c1.x * Cell.CELL_SIZE + Cell.CELL_SIZE / 2;
        int y1 = edge.c1.y * Cell.CELL_SIZE + Cell.CELL_SIZE / 2;
        int x2 = edge.c2.x * Cell.CELL_SIZE + Cell.CELL_SIZE / 2;
        int y2 = edge.c2.y * Cell.CELL_SIZE + Cell.CELL_SIZE / 2;

        int edgeWidth = Math.abs(x1 - x2) + 2;
        int edgeHeight = Math.abs(y1 - y2) + 2;

        int edgeX = (x1 + x2) / 2;
        int edgeY = (y1 + y2) / 2;

        WorldImage wall = new RectangleImage(edgeWidth, edgeHeight, OutlineMode.SOLID, Color.BLACK);
        scene.placeImageXY(wall, edgeX, edgeY);
      }
    }

    if (isSolved) {
      for (Cell cell : solution) {
        WorldImage cellImage;
        if (cell.x == 0 && cell.y == 0) {
          cellImage = new RectangleImage(Cell.CELL_SIZE, Cell.CELL_SIZE, OutlineMode.SOLID,
                  Color.GREEN);
        }
        else if (cell.x == width - 1 && cell.y == height - 1) {
          cellImage = new RectangleImage(Cell.CELL_SIZE, Cell.CELL_SIZE, OutlineMode.SOLID,
                  Color.MAGENTA);
        }
        else {
          cellImage = new RectangleImage(Cell.CELL_SIZE, Cell.CELL_SIZE, OutlineMode.SOLID,
                  Color.YELLOW);
        }
        scene.placeImageXY(cellImage, cell.x * Cell.CELL_SIZE + Cell.CELL_SIZE / 2,
                cell.y * Cell.CELL_SIZE + Cell.CELL_SIZE / 2);

        if (!(cell.x == 0 && cell.y == 0) && !(cell.x == width - 1 && cell.y == height - 1)) {
          List<Cell> solutionList = new ArrayList<>(solution);
          WorldImage orderText = new TextImage(Integer.toString(solutionList.indexOf(cell) + 1),
                  Cell.CELL_SIZE / 2, FontStyle.REGULAR, Color.BLACK);
          scene.placeImageXY(orderText, cell.x * Cell.CELL_SIZE + Cell.CELL_SIZE / 2,
                  cell.y * Cell.CELL_SIZE + Cell.CELL_SIZE / 2);
        }
      }
    }

    WorldImage statusText = new TextImage(status, 13, FontStyle.REGULAR, Color.BLACK);
    scene.placeImageXY(statusText, width * Cell.CELL_SIZE / 2, height * Cell.CELL_SIZE - 20);

    return scene;
  }
}

// class representing the cell
class Cell {
  static final int CELL_SIZE = 30;
  int x;
  int y;
  Cell parent;

  Cell(int x, int y) {
    this.x = x;
    this.y = y;
    this.parent = this;
  }

  // method to draw the cell
  WorldImage draw() {
    return new RectangleImage(CELL_SIZE, CELL_SIZE, OutlineMode.SOLID, Color.GRAY);
  }

  // method that return the correct parent cell
  Cell find() {
    if (this.parent != this) {
      this.parent = this.parent.find();
    }
    return this.parent;
  }

  void union(Cell other) {
    this.find().parent = other.find();
  }
}

// class representing edge of the cell
class Edge {
  Cell c1;
  Cell c2;
  boolean removed;

  Edge(Cell c1, Cell c2) {
    this.c1 = c1;
    this.c2 = c2;
    this.removed = false;
  }
}

// class representing the tests and examples
class ExamplesMazeWorld {
  MazeWorld mazeWorld;

  void testMazeWorld(Tester t) {
    MazeWorld world = new MazeWorld(20, 20);
    world.bigBang(30 * Cell.CELL_SIZE, 30 * Cell.CELL_SIZE, 0.001);
  }

  // test the method generateMaze
  void testGenerateMaze(Tester t) {
    int mazeWidth = 4;
    int mazeHeight = 4;
    MazeWorld world = new MazeWorld(mazeWidth, mazeHeight);
    t.checkExpect(world.cells.size(), mazeHeight);
    t.checkExpect(world.cells.get(0).size(), mazeWidth);
    t.checkExpect(world.edges.size(), 2 * mazeWidth * mazeHeight - mazeWidth - mazeHeight);
  }

  // test draw
  void testDraw(Tester t) {
    Cell cell = new Cell(0, 0);
    WorldImage img = cell.draw();
    WorldImage expectedImage = new RectangleImage(Cell.CELL_SIZE, Cell.CELL_SIZE, OutlineMode.SOLID,
            Color.GRAY);
    t.checkExpect(img.equals(expectedImage), true);
  }

  // test the Kruskal
  void testKruskal(Tester t) {
    int mazeWidth = 4;
    int mazeHeight = 4;
    MazeWorld world = new MazeWorld(mazeWidth, mazeHeight);
    world.kruskal();
    int removedEdgesCount = 0;
    for (Edge edge : world.edges) {
      if (edge.removed) {
        removedEdgesCount++;
      }
    }
    t.checkExpect(removedEdgesCount, mazeWidth * mazeHeight - 1);
  }

  // test for find
  void testFind(Tester t) {
    Cell cell1 = new Cell(0, 0);
    Cell cell2 = new Cell(1, 0);
    t.checkExpect(cell1.find(), cell1);
    t.checkExpect(cell2.find(), cell2);

    cell1.parent = cell2;
    t.checkExpect(cell1.find(), cell2);
    t.checkExpect(cell2.find(), cell2);
  }

  // test for union data
  void testUnion(Tester t) {
    Cell cell1 = new Cell(0, 0);
    Cell cell2 = new Cell(1, 0);
    t.checkExpect(cell1.parent, cell1);
    t.checkExpect(cell2.parent, cell2);

    cell1.union(cell2);
    t.checkExpect(cell1.parent, cell2);
    t.checkExpect(cell2.parent, cell2);
  }

  void init() {
    mazeWorld = new MazeWorld(10, 10);
  }

  // Test for isConnected
  void testIsConnected(Tester t) {
    init();

    // Manually create a connection between two cells
    Cell c1 = mazeWorld.cells.get(0).get(0);
    Cell c2 = mazeWorld.cells.get(0).get(1);
    c1.union(c2);

    // Check
    // t.checkExpect(mazeWorld.isConnected(c1, c2), false);

    // Check if isConnected returns true for connected cells
    Cell c3 = mazeWorld.cells.get(1).get(0);
    t.checkExpect(mazeWorld.isConnected(c1, c3), true);
  }

  // Test for solveDFS and solveBFS
  void testSolveDFSAndSolveBFS(Tester t) {
    init();

    // Testing solveDFS
    mazeWorld.solveDFS(mazeWorld.cells.get(0).get(0));
    t.checkExpect(mazeWorld.isSolved, true);

    // Verify that the solution is valid
    t.checkExpect(isValidSolution(mazeWorld), true);

    // Testing solveBFS
    mazeWorld.isSolved = false;
    mazeWorld.visited = new boolean[mazeWorld.height][mazeWorld.width];
    mazeWorld.solution.clear();
    mazeWorld.solveBFS();
    t.checkExpect(mazeWorld.isSolved, true);

    // Verify that the solution is valid
    t.checkExpect(isValidSolution(mazeWorld), true);
  }

  // Test for onKeyEvent
  void testOnKeyEvent(Tester t) {
    init();

    // Testing 'd' key press for selecting DFS
    mazeWorld.onKeyEvent("d");
    t.checkExpect(mazeWorld.useDFS, true);

    // Testing 'b' key press for selecting BFS
    mazeWorld.onKeyEvent("b");
    t.checkExpect(mazeWorld.useDFS, false);

    // Testing 'n' key press for generating a new maze
    ArrayList<ArrayList<Cell>> oldCells = mazeWorld.cells;
    mazeWorld.onKeyEvent("n");
    t.checkExpect(oldCells.equals(mazeWorld.cells), true);

    // Testing 's' key press for solving the maze
    mazeWorld.useDFS = true; // Using DFS for solving the maze
    mazeWorld.onKeyEvent("s");
    t.checkExpect(mazeWorld.isSolved, true);

    // Verify that the solution is valid
    t.checkExpect(isValidSolution(mazeWorld), true);
  }

  // Helper method to check if a solution is valid
  boolean isValidSolution(MazeWorld mazeWorld) {
    Cell previousCell = null;
    for (Cell cell : mazeWorld.solution) {
      if (previousCell != null) {
        if (!mazeWorld.isConnected(previousCell, cell)) {
          return false;
        }
      }
      previousCell = cell;
    }
    return true;
  }

}