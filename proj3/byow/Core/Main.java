package byow.Core;

import byow.InputDemo.InputSource;
import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.algs4.StdDraw;
import edu.princeton.cs.algs4.WeightedQuickUnionUF;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** This is the main entry point for the program. This class simply parses
 *  the command line inputs, and lets the byow.Core.Engine class take over
 *  in either keyboard or input string mode.
 */
public class Main {
    private static final int WIDTH = 50;
    private static final int HEIGHT = 50;
    private static final long SEED = 3080248; //user input somehow
    private static final Random RANDOM = new Random(SEED);


    public static void main(String[] args) {
        if (args.length > 2) {
            System.out.println("Can only have two arguments - the flag and input string");
            System.exit(0);
        } else if (args.length == 2 && args[0].equals("-s")) {
            Engine engine = new Engine();
            engine.interactWithInputString(args[0]);
            System.out.println(engine.toString());
        } else {
            Engine engine = new Engine();
            engine.interactWithKeyboard();
        }
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        TETile[][] world = new TETile[WIDTH][HEIGHT];
        drawWorld(world);
        insertAvatar(world);

        Avatar player1 = null;
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                if (world[i][j] == Tileset.AVATAR) {
                    player1 = new Avatar(i, j);
                }
            }
        }

        ter.renderFrame(world);

        player1.moveAvatar(world, ter);
    }

    static class Room{
        Position p;
        int length;
        int width;
        public Room(Position p, int length, int width){
            this.p = p;
            this.length = length;
            this.width = width;
        }
        public Position randPosition(){
            //int p1_x = RANDOM.nextInt(this.p.x, this.p.x + this.width - 1);
            //int p1_y = RANDOM.nextInt(this.p.y - this.length + 1, this.p.y);
            int p1_x = RANDOM.nextInt(this.p.x+ (this.width/2)-1, this.p.x + (this.width/2)+1);
            int p1_y = RANDOM.nextInt(this.p.y - (this.length/2) - 1, this.p.y - (this.length/2) + 1);
            return new Position(p1_x, p1_y);
        }
    }
    static class Hallway{
        Position startingP;
        Position endingP;
        public Hallway(Position startingP, Position endingP){
            this.startingP = startingP;
            this.endingP = endingP;
        }
    }

    //add avatar to the board
    public static void insertAvatar(TETile[][] tiles) {
        boolean avatarCreated = false;
        while (!avatarCreated) {
            int randomX = RANDOM.nextInt(0, WIDTH);
            int randomY = RANDOM.nextInt(0, HEIGHT);
            TETile currentTile = tiles[randomX][randomY];
            if (currentTile.equals(Tileset.FLOOR)) {
                tiles[randomX][randomY] = Tileset.AVATAR;
                avatarCreated = true;
            }
        }
    }
    // draw row to the board at anchor
    public static void drawRow(TETile[][] tiles, Position p, TETile tile, int length){
        for(int dx = 0; dx < length; dx++) {
            tiles[p.x + dx][p.y] = tile;
        }
    }

    public static boolean checkEmpty(TETile[][] tiles, Position p, int length, int width){
        //if(p.x - 1 < 0 || p.x + width + 1 >= 50 || p.y < 0 || p.y + length + 2 >= 50){
        //return false;
        //}
        Position positionP = p.shift(-1, 0);
        for (int i = 0; i < length + 1; i++){
            for (int j = 0; j < width + 1; j++){
                if(tiles[positionP.x + i][positionP.y - j] != Tileset.NOTHING){
                    return false;
                }
            }
        }
        return true;
    }
    public static boolean generateRoom(TETile[][] tiles, Position p, int length, int width) {
        if(checkEmpty(tiles, p, length, width)) {
            Position positionP = p.shift(0, 0);
            Position topWall = positionP.shift(-1, 1);
            drawRow(tiles, topWall, Tileset.WALL, width + 2);
            Position bottomWall = positionP.shift(-1, -length);
            drawRow(tiles, bottomWall, Tileset.WALL, width + 2);
            for (int i = 0; i < length; i++) {
                Position currRow = positionP.shift(0, -i);
                Position leftWall = positionP.shift(-1, -i);
                Position rightWall = positionP.shift(width, -i);
                drawRow(tiles, leftWall, Tileset.WALL, 1);
                drawRow(tiles, currRow, Tileset.FLOOR, width);
                drawRow(tiles, rightWall, Tileset.WALL, 1);
            }
            return true;
        }
        return false;
    }
    public static void generateHallway(TETile[][] tiles, List<Room> rooms, List<Hallway> hallList) {
        WeightedQuickUnionUF uf = new WeightedQuickUnionUF(rooms.size());
        boolean notConnected = true;
        while(notConnected){
            notConnected = false;
            int bestI = -1;
            int bestJ = -1;
            int bestDist = 1000000;
            for(int i = 0; i < rooms.size(); i++){
                for (int j = 0; j < rooms.size(); j++){
                    if(uf.find(i) != uf.find(j)){
                        int dist = rooms.get(i).p.distance(rooms.get(j).p);
                        if(dist < bestDist){
                            bestI = i;
                            bestJ = j;
                            bestDist = dist;
                            notConnected = true;
                        }
                    }
                }
            }
            if(notConnected){
                hallList.add(connectRooms(tiles, rooms.get(bestI), rooms.get(bestJ)));
                uf.union(bestI, bestJ);
            }
        }
    }

    public static void clearRooms(TETile[][] tiles, List<Room> rooms){
        for(int i = 0; i < rooms.size(); i++){
            Room room = rooms.get(i);
            Position positionP = rooms.get(i).p.shift(0, 0);
            for (int j = 0; j < rooms.get(i).length; j++) {
                Position currRow = positionP.shift(0, -j);
                drawRow(tiles, currRow, Tileset.FLOOR, rooms.get(i).width);
            }
        }

    }

    public static void clearHalls(TETile[][] tiles, List<Hallway> hallways){
        for(Hallway hall: hallways){
            if(hall.endingP.x > hall.startingP.x) {
                for(int i = 0; i <= (hall.endingP.x - hall.startingP.x); i++){
                    tiles[hall.startingP.x + i][hall.startingP.y] = Tileset.FLOOR;
                }
            } else {
                for(int i = 0; i <= (hall.startingP.x - hall.endingP.x); i++){
                    tiles[hall.startingP.x - i][hall.startingP.y] = Tileset.FLOOR;
                }
            }
            if(hall.endingP.y >= hall.startingP.y) {
                for (int j = 0; j < (hall.endingP.y - hall.startingP.y); j++) {
                    tiles[hall.endingP.x][hall.startingP.y + j] = Tileset.FLOOR;
                }
            } else {
                for (int j = 0; j <= (hall.startingP.y - hall.endingP.y); j++) {
                    tiles[hall.endingP.x][hall.startingP.y - j] = Tileset.FLOOR;
                }
            }
        }

    }

    public static void drawWorld(TETile[][] tiles){
        fillWithNothingTiles(tiles);
        int numRooms = RANDOM.nextInt(15,500);
        ArrayList<Room> roomList = new ArrayList<Room>();
        ArrayList<Hallway> hallList = new ArrayList<Hallway>();
        for(int i = 0; i < numRooms; i++){
            Position p = new Position(RANDOM.nextInt(1, 38),RANDOM.nextInt(12, 49));
            int length = RANDOM.nextInt(3,8);
            int width = RANDOM.nextInt(3,8);
            if(generateRoom(tiles, p, length, width)){
                roomList.add(new Room(p, length, width));
            }
        }
        generateHallway(tiles, roomList, hallList);
        clearRooms(tiles, roomList);
        clearHalls(tiles, hallList);

    }

    public static Hallway connectRooms(TETile[][] tiles, Room first, Room second){
        Hallway hall1 = new Hallway(first.randPosition(), second.randPosition());

        if(hall1.endingP.x > hall1.startingP.x) {
            for(int i = 0; i <= 1 + (hall1.endingP.x - hall1.startingP.x); i++){
                tiles[hall1.startingP.x + i][hall1.startingP.y+1] = Tileset.WALL;
                //tiles[hall1.startingP.x + i][hall1.startingP.y] = Tileset.FLOOR;
                tiles[hall1.startingP.x + i][hall1.startingP.y-1] = Tileset.WALL;
            }

        } else {
            for(int i = 0; i <= 1 + (hall1.startingP.x - hall1.endingP.x); i++){
                tiles[hall1.startingP.x - i][hall1.startingP.y+1] = Tileset.WALL;
                //tiles[hall1.startingP.x - i][hall1.startingP.y] = Tileset.FLOOR;
                tiles[hall1.startingP.x - i][hall1.startingP.y-1] = Tileset.WALL;
            }
        }
        if(hall1.endingP.y >= hall1.startingP.y) {
            for (int j = 0; j < (hall1.endingP.y - hall1.startingP.y); j++) {
                tiles[hall1.endingP.x - 1][hall1.startingP.y + j] = Tileset.WALL;
                tiles[hall1.endingP.x][hall1.startingP.y + j] = Tileset.FLOOR;
                tiles[hall1.endingP.x + 1][hall1.startingP.y + j] = Tileset.WALL;
            }
        } else {
            for (int j = 0; j <= (hall1.startingP.y - hall1.endingP.y); j++) {
                tiles[hall1.endingP.x - 1][hall1.startingP.y - j] = Tileset.WALL;
                tiles[hall1.endingP.x][hall1.startingP.y - j] = Tileset.FLOOR;
                tiles[hall1.endingP.x + 1][hall1.startingP.y - j] = Tileset.WALL;
            }
        }
        return hall1;
    }

    /**
     * Fills the given 2D array of tiles with blank tiles.
     * @param tiles
     */
    public static void fillWithNothingTiles(TETile[][] tiles) {
        int height = tiles[0].length;
        int width = tiles.length;
        for (int x = 0; x < width; x += 1) {
            for (int y = 0; y < height; y += 1) {
                tiles[x][y] = Tileset.NOTHING;
            }
        }
    }

    //helper class to deal with positions
    private static class Position {
        int x;
        int y;
        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int distance(Position otherP){
            return Math.abs(x - otherP.x) + Math.abs(y - otherP.y);
        }

        public Position shift(int dx, int dy) {
            return new Position(this.x + dx, this.y + dy);
        }
    }

    public interface InputSource {
        public char getNextKey();
        public boolean possibleNextInput();
    }

    public static void moveCharacter(TETile[][] tiles, Avatar avi, char input) {
        int currX = avi.x;
        int currY = avi.y;
        if (input == 'w') {
            avi.y = avi.y + 1;
            //this.p =
        }
        if (input == 'a') {
            avi.x = avi.x - 1;
        }
        if (input == 's') {
            avi.y = avi.y - 1;
        }
        if (input == 'd') {
            avi.x = avi.x + 1;
        }
        tiles[avi.x][avi.y] = Tileset.AVATAR;
        tiles[currX][currY] = Tileset.FLOOR;
    }

    private static class Avatar {
        int x;
        int y;
        Position p;
        TETile design;

        Avatar(int x, int y) {
            this.design = Tileset.AVATAR;
            this.x = x;
            this.y = y;
            this.p = new Position(this.x, this.y);
        }

        private void moveAvatar(TETile[][] tiles, TERenderer renderer) {
            while (true) {
                if (StdDraw.hasNextKeyTyped()) {
                    char move = StdDraw.nextKeyTyped();
                    if (move == 'q') {
                        System.exit(0);
                    }
                    moveCharacter(tiles, this, move);
                    renderer.renderFrame(tiles);
                }
            }
        }
    }


}
