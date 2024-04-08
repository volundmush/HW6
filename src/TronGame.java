/*
 * Author:  Andrew Bastien
 * Email: abastien2021@my.fit.edu
 * Course:  CSE 2010
 * Section: 23
 * Term: Spring 2024
 * Project: HW6, Tron Game
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TronGame {
    private final Scanner data;
    private int rows = 0;
    private int columns = 0;
    private int mode = 0;
    private GridLocation[][] grid = null;


    public TronGame(final Scanner data, int mode) {
        this.data = data;
        this.mode = mode;
    }

    private enum EntityType {
        // name is always Tron, represented by a T.
        Tron,
        // name is always IOTower, represented by an I.
        IOTower,
        // Name is a lowercase alphabetic letter like a b c d e f, also used to draw it on grid.
        GridBug,
        Obstacle
    }

    private enum GameState {
        Running,
        Win,
        Lose
    }

    private GameState state = GameState.Running;

    private static class Entity {
        public String name;
        public EntityType type;
        public GridLocation location;

        public Entity(String name, EntityType type, GridLocation location) {
            this.name = name;
            this.type = type;
            this.location = location;
        }

        public String toString() {
            switch (type) {
                case Tron:
                    return "T";
                case IOTower:
                    return "I";
                case GridBug:
                    return name;
                case Obstacle:
                    return "#";
                default:
                    return "?";
            }
        }

        public boolean canMoveTo(GridLocation loc) {
            // Tron can move to the IOTower or an empty space.
            // Bugs can move to Tron or an empty space.
            if(loc.containedEntity == null) return true;
            switch(type) {
                case Tron:
                    return loc.containedEntity.type == EntityType.IOTower;
                case GridBug:
                    return loc.containedEntity.type == EntityType.Tron;
                default:
                    return false;
            }
        }

        public void printShortestPath(LinkedList<GridLocation> path) {
            String direction;
            // Let's determine if path's first element is u, d, l, or r from current location.
            GridLocation next = path.get(1);
            if(next.x < location.x) {
                direction = "u";
            } else if(next.x > location.x) {
                direction = "d";
            } else if(next.y < location.y) {
                direction = "l";
            } else {
                direction = "r";
            }
            String coordinatesArray = String.join(" ", path.stream().map(GridLocation::printCoordinates).toArray(String[]::new));
            System.out.println(String.format("Bug %s: %s %d %s", this.name, direction, path.size()-1, coordinatesArray));
        }

    }

    private final HashMap<String, Entity> entities = new HashMap<>();
    private final LinkedList<Entity> gridBugs = new LinkedList<>();

    private static class GridLocation {
        // class for the Vertex/Node of the grid.
        public Entity containedEntity;

        // the Directions are always up, down, left, or right.
        public HashMap<String, GridLocation> directions = new HashMap<>();

        public int x, y;

        public String toString() {
            if (containedEntity != null) {
                return containedEntity.toString();
            } else {
                return " ";
            }
        }

        public String printCoordinates() {
            return String.format("(%d, %d)", x, y);
        }
    }

    private void getDimensions() {
        // First line is the dimensions of the grid. rows then columns.
        String[] parts = data.nextLine().split(" ");
        rows = Integer.parseInt(parts[0]);
        columns = Integer.parseInt(parts[1]);
    }

    private void setupGrid() {
        // for loop number of rows and use data.nextLine(). Each row is a sequence of characters that should be columns long.
        // It is either a space, T, I, #, or a lowercase letter which will be used to construct Entities.

        // The game grid is represented as graph, with each vertex representing a cell in the grid. The GridLocation class
        // has a HashMap<String, GridLocation> directions that contains the adjacent vertices in the grid, with keys as
        // up / down / left / right. The GridLocation class also has a containedEntity field that can be null or contain
        // a single entity.

        // Step 1: Create a 2D Array for temporary storage/setup...
        grid = new GridLocation[rows][columns];

        // Populate the grid
        for (int i = 0; i < rows; i++) {
            String line = data.nextLine();
            for (int j = 0; j < columns; j++) {
                char c = line.charAt(j);
                GridLocation location = new GridLocation();
                location.x = i;
                location.y = j;
                Entity ent = null;
                switch (c) {
                    case 'T':
                        ent = new Entity("Tron", EntityType.Tron, location);
                        entities.put("Tron", ent);
                        break;
                    case 'I':
                        ent = new Entity("IOTower", EntityType.IOTower, location);
                        entities.put("IOTower", ent);
                        break;
                    case '#':
                        ent = new Entity("Obstacle", EntityType.Obstacle, location);
                        entities.put("Obstacle", ent);
                        break;
                    case ' ': // space
                        break;
                    default:
                        ent = new Entity(String.valueOf(c), EntityType.GridBug, location);
                        entities.put(String.valueOf(c), ent);
                        gridBugs.add(ent);
                        break;
                }
                if(ent != null) {
                    location.containedEntity = ent;
                }
                grid[i][j] = location;
            }
        }

        // Sort gridBugs by name. Alphabetical order, starting with a moving towards z.
        gridBugs.sort((a, b) -> a.name.compareTo(b.name));

        // Link adjacent locations
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                GridLocation current = grid[i][j];
                if (i > 0) current.directions.put("u", grid[i-1][j]);
                if (i < rows - 1) current.directions.put("d", grid[i+1][j]);
                if (j > 0) current.directions.put("l", grid[i][j-1]);
                if (j < columns - 1) current.directions.put("r", grid[i][j+1]);
            }
        }

    }

    private void printGrid() {
        // the first line should look like " 0123456789" where the numbers are the column indices...
        System.out.print("  ");
        for (int i = 0; i < columns; i++) {
            System.out.print(i % 10);
        }
        System.out.println();

        // the next rows should look like "n eeeeee" where n is row indices are the row indices and the # are the entities or open spaces.
        for (int i = 0; i < rows; i++) {
            System.out.print(i);
            System.out.print(" ");
            for (int j = 0; j < columns; j++) {
                System.out.print(grid[i][j].toString());
            }
            System.out.println();
        }
    }

    private boolean moveTron(String direction) {
        // Move Tron in the specified direction if possible. Only one entity may exist in a location.
        // Exception for the IO tower, or a bug reaching Tron.
        Entity tron = entities.get("Tron");
        GridLocation tloc = tron.location;
        GridLocation next = tloc.directions.get(direction);
        if (next != null) {
            if(next.containedEntity != null) {
                if(next.containedEntity.type == EntityType.IOTower) {
                    state = GameState.Win;
                } else if(next.containedEntity.type == EntityType.GridBug) {
                    state = GameState.Lose;
                } else {
                    return false;
                }
            }
            tloc.containedEntity = null;
            next.containedEntity = tron;
            tron.location = next;
            return true;
        }
        return false;
    }

    private void moveBugs() {
        // for each bug in GridBugs, move it in the direction of Tron if possible. Only one entity may exist in a location.

        // first let's get Tron...
        Entity tron = entities.get("Tron");
        GridLocation tloc = tron.location;

        // We already sorted the Grid Bugs so we'll just for-each it...
        for (Entity bug : gridBugs) {
            GridLocation bLoc = bug.location;
            LinkedList<GridLocation> path = findShortestPath(bug, tron);
            if (path.size() > 1) {
                // Move the bug to the next location in the path
                GridLocation next = path.get(1);
                if(next.containedEntity != null && next.containedEntity.type == EntityType.Tron) {
                    state = GameState.Lose;
                    bLoc.containedEntity = null;
                    next.containedEntity = bug;
                    return;
                }
                bLoc.containedEntity = null;
                next.containedEntity = bug;
                bug.location = next;
            }
        }

    }

    private LinkedList<GridLocation> breadthFirstSearch(Entity from, Entity to) {
        // Check if the start and end locations are the same
        if (from.equals(to)) {
            LinkedList<GridLocation> path = new LinkedList<>();
            path.add(from.location);
            return path;
        }

        Queue<GridLocation> queue = new LinkedList<>();
        Map<GridLocation, GridLocation> parentMap = new HashMap<>();
        Set<GridLocation> visited = new HashSet<>();

        queue.add(from.location);
        visited.add(from.location);

        // BFS loop
        while (!queue.isEmpty()) {
            GridLocation current = queue.remove();

            // Check if we've reached our destination
            if (current.equals(to.location)) {
                // If so, reconstruct and return the path
                return reconstructPath(parentMap, to.location);
            }

            // Visit the neighbors in the specified order
            visitInOrder(current, "u", visited, parentMap, queue, from);
            visitInOrder(current, "d", visited, parentMap, queue, from);
            visitInOrder(current, "l", visited, parentMap, queue, from);
            visitInOrder(current, "r", visited, parentMap, queue, from);
        }

        // If we reach here, no path exists
        return new LinkedList<>();
    }

    private LinkedList<GridLocation> findCheapestPath(Entity from, Entity to) {
        // Preliminary checks
        if (from.equals(to)) {
            LinkedList<GridLocation> path = new LinkedList<>();
            path.add(from.location);
            return path;
        }

        // Map to track the cheapest cost to reach a GridLocation
        Map<GridLocation, Integer> costs = new HashMap<>();
        // Parent map to reconstruct the path later
        Map<GridLocation, GridLocation> parentMap = new HashMap<>();
        // Use a PriorityQueue to process nodes in order of their current cheapest cost
        PriorityQueue<GridLocation> queue = new PriorityQueue<>(Comparator.comparingInt(costs::get));

        // Initialize with the starting location
        costs.put(from.location, 0);
        queue.add(from.location);

        // Keep track of visited nodes to avoid reprocessing
        Set<GridLocation> visited = new HashSet<>();

        // Calculate Tron's path for dynamic weighting
        LinkedList<GridLocation> tronPath = breadthFirstSearch(from, entities.get("Tron"));

        while (!queue.isEmpty()) {
            GridLocation current = queue.poll();
            visited.add(current);

            // Destination reached
            if (current.equals(to.location)) {
                return reconstructPath(parentMap, to.location);
            }

            // Process each neighbor
            for (Map.Entry<String, GridLocation> entry : current.directions.entrySet()) {
                GridLocation neighbor = entry.getValue();

                // Skip if neighbor is invalid or visited
                if (neighbor == null || visited.contains(neighbor) || !from.canMoveTo(neighbor)) {
                    continue;
                }

                // Calculate cost dynamically based on Tron's path
                int costToNeighbor = costs.get(current) + calculateCost(current, neighbor, tronPath);
                if (!costs.containsKey(neighbor) || costToNeighbor < costs.get(neighbor)) {
                    costs.put(neighbor, costToNeighbor);
                    parentMap.put(neighbor, current);
                    // Priority queue does not automatically reorder its elements when their cost changes, so we re-add the neighbor
                    queue.add(neighbor);
                }
            }
        }

        // No path found
        return new LinkedList<>();
    }

    private int calculateCost(GridLocation from, GridLocation to, LinkedList<GridLocation> tronPath) {
        boolean fromOnPath = tronPath.contains(from);
        boolean toOnPath = tronPath.contains(to);

        if (fromOnPath && toOnPath) {
            return 1;
        } else if (fromOnPath || toOnPath) {
            return 2;
        } else {
            return 3;
        }
    }

    // Some overloads for the findShortestPath method
    private LinkedList<GridLocation> findShortestPath(Entity from, Entity to) {
        if(mode == 2) {
            return findCheapestPath(from, to);
        } else {
            return breadthFirstSearch(from, to);
        }
    }

    private void visitInOrder(GridLocation current, String direction, Set<GridLocation> visited,
                              Map<GridLocation, GridLocation> parentMap, Queue<GridLocation> queue, Entity from) {
        GridLocation neighbor = current.directions.get(direction);
        // Check if the neighbor is valid and not visited
        if (neighbor != null && !visited.contains(neighbor) && from.canMoveTo(neighbor)) {
            visited.add(neighbor);
            parentMap.put(neighbor, current); // Map neighbor to the current node as its parent
            queue.add(neighbor);
        }
    }

    private LinkedList<GridLocation> reconstructPath(Map<GridLocation, GridLocation> parentMap, GridLocation to) {
        LinkedList<GridLocation> path = new LinkedList<>();
        for (GridLocation at = to; at != null; at = parentMap.get(at)) {
            path.addFirst(at); // Add to the beginning of the list to reverse the path
        }
        return path;
    }

    private void runGame() {
        // create a Scanner that reads from keyboard/CLI...
        Scanner input = new Scanner(System.in);

        while(state == GameState.Running) {
            printGrid();

            String direction = "a";
            while (Objects.equals(direction, "a")) {
                System.out.print("Please enter your move [u(p), d(own), l(eft), or r(ight)]: ");
                direction = input.nextLine();
                System.out.println(direction);
                if(!moveTron(direction)) {
                    direction = "a";
                }
            }
            System.out.println();

            // mode 0 only handles a single turn.
            if(mode == 0) {
                printGrid();
                System.out.println();

                Entity tron = entities.get("Tron");

                for(Entity bug : gridBugs) {
                    LinkedList<GridLocation> path = findShortestPath(bug, tron);
                    bug.printShortestPath(path);
                }

                return;
            }

            moveBugs();

            printGrid();

        }

        switch(state) {
            case Win:
                System.out.println("Tron reaches I/O Tower");
                break;
            case Lose:
                System.out.println("A bug is not hungry any more!");
                break;
            default:
                System.out.println("Game ended in an unknown state.");
        }


    }

    public void run() {
        // First, establish dimensions.
        getDimensions();

        // next, let's construct the grid and entities.
        setupGrid();

        // Finally, we can start the game loop.
        runGame();

    }

    public static void launch(String[] args, int defaultMode) {
        if (args.length < 1) {
            System.out.println("No file path provided.");
            return;
        }

        int mode = defaultMode;
        if(args.length > 1) {
            mode = Integer.parseInt(args[1]);
        }

        // use java.util.Scanner because dang this is complicated.
        try {
            Scanner data = new Scanner(new File(args[0]), StandardCharsets.US_ASCII.name());
            TronGame program = new TronGame(data, mode);
            program.run();
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + args[0]);
        }
    }
}
