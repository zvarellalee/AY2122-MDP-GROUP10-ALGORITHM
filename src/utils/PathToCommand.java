package utils;

import algorithms.ArcMove;
import algorithms.FastestPathAlgo;
import algorithms.MoveType;
import algorithms.TripPlannerAlgo;
import map.Arena;
import map.MapConstants;
import map.PictureObstacle;
import robot.Robot;
import robot.RobotConstants;
import utils.CommConstants.INSTRUCTION_TYPE;

import java.util.ArrayList;

public class PathToCommand {
    static Robot bot = new Robot(RobotConstants.ROBOT_INITIAL_CENTER_COORDINATES, RobotConstants.ROBOT_DIRECTION.NORTH, false);
    static Arena arena = new Arena(bot);
    static CommMgr comm = CommMgr.getCommMgr();

    static FastestPathAlgo fast = new FastestPathAlgo(arena);
    static TripPlannerAlgo algo = new TripPlannerAlgo(arena);

    public static void main(String[] args) {
        arena.addPictureObstacle(18, 5, MapConstants.IMAGE_DIRECTION.NORTH);
        //arena.addPictureObstacle(10, 15, MapConstants.IMAGE_DIRECTION.WEST);
        //arena.addPictureObstacle(10, 13, MapConstants.IMAGE_DIRECTION.WEST);
        arena.addPictureObstacle(15, 10, MapConstants.IMAGE_DIRECTION.SOUTH);
        //arena.addPictureObstacle(2, 11, MapConstants.IMAGE_DIRECTION.SOUTH);

        int[] path = fast.planFastestPath();
        //TARGET,obstalceNum,TARGETid
        //comm.sendMsg(":AND:");

        comm.connectToRPi();
        //comm.sendMsg(":STM:0008");
        doThePath(path);

        comm.endConnection();


    }

    private static void doThePath(int[] path) {
        algo.constructMap();
        ArrayList<PictureObstacle> map = arena.getObstacles();
        Robot r = arena.getRobot();
        int startX = r.getX();
        int startY = r.getY();
        int startAngle = r.getRobotDirectionAngle();
        PictureObstacle next;
        ArrayList<MoveType> arrayList;
        int count = 0;
        for (int i : path) {
            next = map.get(i);
            System.out.println("---------------Path " + count + "---------------");
            System.out.println(next.getX() + ", " + next.getY());
            arrayList = algo.planPath(startX, startY, startAngle, next.getX(), next.getY(), next.getImadeDirectionAngle(), RobotConstants.TURN_RADIUS, true, true);
            sendMovesToRobot(arrayList);
            int[] coords = algo.getReverseCoordinates(next);
            startX = coords[0];
            startY = coords[1];
            startAngle = coords[2];
            count++;
        }
    }

    private static void sendMovesToRobot(ArrayList<MoveType> moveList) {
        String formatted;
        String msg;
        INSTRUCTION_TYPE instructionType = null;

        sendToRobot(":STM:0008");

        for (MoveType move : moveList) {
            int measure = 0;
            if (move.isLine()) {
                measure = (int) move.getLength();
                formatted = String.format("%03d", measure);
                if (move.isReverse()) {
                    instructionType = INSTRUCTION_TYPE.BACKWARD;
                } else {
                    instructionType = INSTRUCTION_TYPE.FORWARD;
                }
                msg = ":STM:" + formatted + INSTRUCTION_TYPE.encode(instructionType);
            } else {
                ArcMove moveConverted = (ArcMove) move;
                if (moveConverted.isTurnLeft()) {
                    instructionType = INSTRUCTION_TYPE.FORWARD_LEFT;
                } else {
                    instructionType = INSTRUCTION_TYPE.FORWARD_RIGHT;
                }
                msg = ":STM:090" + INSTRUCTION_TYPE.encode(instructionType);
            }
            sendToRobot(msg);
        }
        takeImage();
    }

    private static void sendToRobot(String cmd) {
        comm.sendMsg(cmd);
        String receiveMsg = null;
        while (receiveMsg == null || !receiveMsg.equals("A")) {
            receiveMsg = comm.recieveMsg();
            //System.out.println("inside loop");
        }
        System.out.println("Message: " + receiveMsg + "\n");
    }

    private static void takeImage() {
        comm.sendMsg(":IMG:scan");
        String receiveMsg = null;
        while (receiveMsg == null || receiveMsg.isEmpty()) {
            receiveMsg = comm.recieveMsg();
            //System.out.println("inside loop");
        }
        System.out.println("Message: " + receiveMsg + "\n");
    }

    private static void moveForwards(int dist) {

    }
}
