/**
 * Diff - compare two files paragraph by paragraph, line by line, token by token or character by character
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

public class Diff {
    private Diff() {  //Class doesn't need instances
    }

    public static void main(String[] args) throws Exception {
        boolean paragraphDiff = false,
                lineDiff = false,
                tokenDiff = false,
                characterDiff = false,
                patchUtil = false;
        FileReader fileOne;
        FileReader fileTwo;

        if (args.length == 3) {
            /* TODO: Basic file validation */
//          Open files for comparison
            fileOne = new FileReader(args[1]);
            fileTwo = new FileReader(args[2]);

//          ArrayLists used for storing the fragmented files
            ArrayList<String> paragraphsOne = null, paragraphsTwo = null;
            ArrayList<String> linesOne = null, linesTwo = null;
            ArrayList<String> tokenOne = null, tokenTwo = null;
            ArrayList<String> charOne = null, charTwo = null;

//          Handle command line agruments
            switch (args[0].charAt(0)) {
                case 'p':
                    paragraphDiff = true;
                    paragraphsOne = new ArrayList<String>(paragraphFragment(fileOne));
                    paragraphsTwo = new ArrayList<String>(paragraphFragment(fileTwo));
                    break;
                case 'l':
                    lineDiff = true;
                    linesOne = new ArrayList<String>(lineFragment(fileOne));
                    linesTwo = new ArrayList<String>(lineFragment(fileTwo));
                    break;
                case 't':
                    tokenDiff = true;
                    tokenOne = new ArrayList<String>(tokenFragment(fileOne));
                    tokenTwo = new ArrayList<String>(tokenFragment(fileTwo));
                    break;
                case 'c':
                    characterDiff = true;
                    charOne = new ArrayList<String>(charFragment(fileOne));
                    charTwo = new ArrayList<String>(charFragment(fileTwo));
                    break;
                case 'P':
                    patchUtil = true;
                    break;
                default:
                    printUsage();
                    break;
            }

            if (paragraphDiff) {
                System.out.println("p");
                ArrayList<int[]> Vs = findSnakes(paragraphsOne, paragraphsTwo);  //Find end points and store each stage of endpoints
                myersDiff(paragraphsOne, paragraphsTwo, Vs);                     //Perform diff and output patch script
            } else if (lineDiff) {
                System.out.println("l");
                ArrayList<int[]> Vs = findSnakes(linesOne, linesTwo);
                myersDiff(linesOne, linesTwo, Vs);
            } else if (tokenDiff) {
                System.out.println("t");
                ArrayList<int[]> Vs = findSnakes(tokenOne, tokenTwo);
                myersDiff(tokenOne, tokenTwo, Vs);
            } else if (characterDiff) {
                System.out.println("c");
                ArrayList<int[]> Vs = findSnakes(charOne, charTwo);
                myersDiff(charOne, charTwo, Vs);
            } else if (patchUtil) {
                patchUtil(fileOne, fileTwo);
            }

        } else {
            printUsage();
        }
    }

    //  Checks all points of the graph for matches and returns the d points at each stage TODO Blow up if no matches
    private static ArrayList<int[]> findSnakes(ArrayList<String> firstInput, ArrayList<String> secondInput) {
        int N = firstInput.size();
        int M = secondInput.size();
        ArrayList<int[]> Vs = new ArrayList<int[]>();

        int[] V = new int[N + M];
        V[1] = 0;
        for (int d = 0; d <= N + M; d++) {
            int[] vClone = new int[N + M];
            for (int k = -d; k <= d; k += 2) {
                // down or right?
                boolean down = (k == -d || (k != d && V[(k - 1 + V.length) % V.length] < V[(k + 1 + V.length) % V.length]));
                int kPrev = down ? k + 1 : k - 1;

                // start point
                int xStart = V[(kPrev + V.length) % V.length];
                int yStart = xStart - ((kPrev + V.length) % V.length);

                // mid point
                int xMid = down ? xStart : xStart + 1;
                int yMid = (xMid - k + V.length) % V.length;

                // end point
                int xEnd = xMid;
                int yEnd = yMid;

                // follow diagonal
                while (xEnd < N && yEnd < M && firstInput.get(xEnd).equals(secondInput.get(yEnd))) {
                    xEnd++;
                    yEnd++;
                }
                // save end point
                V[(k + V.length) % V.length] = xEnd;

                // check for solution
                if (xEnd >= N && yEnd >= M) {
                    Vs.add(d, V.clone());
                    return Vs;/* solution has been found */
                }
            }
            Vs.add(d, V.clone());
        }
        return Vs; //TODO Blow up if no matches
    }

    //    Traces back all the d points in the graph to print out a solution
    private static void myersDiff(ArrayList<String> firstInput, ArrayList<String> secondInput, ArrayList<int[]> Vs) {
        ArrayList<String> editScript = new ArrayList<String>();
        int pX = firstInput.size(), pY = secondInput.size();

        for (int d = Vs.size() - 1; pX > 0 || pY > 0; d--) {
            int[] V = Vs.get(d);
            int k = pX - pY;

            // End of snake contained in V
            int xEnd = V[(k + V.length) % V.length];
            int yEnd = (xEnd - k + V.length) % V.length;

            // down or right?
            boolean down = (k == -d || (k != d && V[(k - 1 + V.length) % V.length] < V[(k + 1 + V.length) % V.length]));
            int kPrev = down ? k + 1 : k - 1;

            // Snake beginning
            int xStart = V[(kPrev + V.length) % V.length];
            int yStart;
            if (d != 0) {
                yStart = (xStart - kPrev + V.length) % V.length;
            } else {
                yStart = xStart - kPrev;       // To handle the starting point where y=-1
            }
            // Middle of Snake - starts going diagonally
            int xMid = down ? xStart : xStart + 1;
            int yMid = (xMid - k + V.length) % V.length;
            if (down && yMid > 0) {   //Going down the graph - insertion
                editScript.add("<~~[" + xMid + " I " + secondInput.get(yMid - 1));
            } else if (!down && yMid > -1) {   //Going right in the graph - deletion
                editScript.add("<~~[" + xMid + " D");
            }
            pX = xStart;
            pY = yStart;
        }

        for (int i = editScript.size() - 1; i >= 0; i--) {
            System.out.println(editScript.get(i));
        }
    }

    //    Print command line usage
    private static void printUsage() {
        System.out.println("Usage: Diff option FILE1 FILE2");
        System.out.println("option:");
        System.out.println("l - perform line comparison");
        System.out.println("p - perform paragraph comparison");
        System.out.println("c - perform character comparison");
        System.out.println("t - perform token comparison");
        System.out.println("P - use patch utility");
    }

    private static void patchUtil(FileReader target, FileReader patchFile) throws Exception {
        ArrayList<Command> commands = new ArrayList<Command>();
        Scanner in = new Scanner(patchFile);
        String tempS;
        String args = "";
        char comm;
        int row;
        char typeDiff = in.nextLine().charAt(0);  //Get type of diff character
        int tempIndexI, tempIndexD;
        in.useDelimiter("<~~\\[");     //New command separator
        while (in.hasNext()) {
            tempS = in.next();

            boolean insertComm = false, deleteComm = false;
            tempIndexI = tempS.indexOf('I');
            tempIndexD = tempS.indexOf('D');
            if (tempIndexD == -1) { //If no 'D' present
                insertComm = true;
            } else if (tempIndexI == -1) { //If no 'I' present
                deleteComm = true;
            } else if (tempIndexI < tempIndexD) { //If D is present somewhere in the fragment, but I is before it
                insertComm = true;
            } else {
                deleteComm = true;
            }

            if (insertComm) { //Command is I (insert)
                comm = 'I';
                row = Integer.parseInt(tempS.substring(0, tempIndexI - 1)); //The line number for the command
                args = tempS.substring(tempIndexI + 2, tempS.length() - 1); //The things to be inserted
            } else {   //Command is D (delete)
                comm = 'D';
                row = Integer.parseInt(tempS.substring(0, tempIndexD - 1)); //The line number for the command
                args = "";
            }
            commands.add(new Command(comm, row, args));
        }

        switch (typeDiff) {
            case 'p':
                ArrayList<String> paragraphHolder = new ArrayList<String>(paragraphFragment(target));  //fragment input
                for (int i = commands.size() - 1; i >= 0; i--) {                                       //execute commands starting from the end
                    Command.execCommand(paragraphHolder, commands.get(i));
                }
                for (int i = 0; i < paragraphHolder.size(); i++) {
                    System.out.print(paragraphHolder.get(i));
                }
                break;
            case 'l':
                ArrayList<String> lineHolder = new ArrayList<String>(lineFragment(target));
                for (int i = commands.size() - 1; i >= 0; i--) {
                    Command.execCommand(lineHolder, commands.get(i));
                }
                for (int i = 0; i < lineHolder.size(); i++) {
                    System.out.println(lineHolder.get(i));
                }
                break;
            case 't':
                ArrayList<String> tokenHolder = new ArrayList<String>(tokenFragment(target));
                for (int i = commands.size() - 1; i >= 0; i--) {
                    Command.execCommand(tokenHolder, commands.get(i));
                }
                for (int i = 0; i < tokenHolder.size(); i++) {
                    System.out.print(tokenHolder.get(i));
                }
                break;
            case 'c':
                ArrayList<String> charHolder = new ArrayList<String>(charFragment(target));
                for (int i = commands.size() - 1; i >= 0; i--) {
                    Command.execCommand(charHolder, commands.get(i));
                }
                for (int i = 0; i < charHolder.size(); i++) {
                    System.out.print(charHolder.get(i));
                }
                break;

        }
    }

    //      Separate all the paragraphs in the input file and put them in an array list
    //      Fragmented on one or more blank lines. (Blank lines are added to the previous parahraph)
    private static ArrayList<String> paragraphFragment(FileReader file) throws Exception {
        int c;  //holds current character
        int previousC = -1; // holds prev character
        String paragraphChunk = "";
        ArrayList<String> output = new ArrayList<String>();
        boolean addMore = false;

        while ((c = file.read()) != -1) {
            if (c == 10 && previousC == 10) {
                addMore = true;
            } else {
                if (addMore) {
                    output.add(paragraphChunk);
                    paragraphChunk = "";
                    addMore = false;
                }
            }
            paragraphChunk += (char) c;   //cast to char and add it to paragraph
            previousC = c;
        }

        output.add(paragraphChunk);
        return output;
    }

    //      Separate all the lines in the input file and put them in an array list
    //      Fragmented by blank lines, on each new line, blank lines added to the last line to perserve formatting
    private static ArrayList<String> lineFragment(FileReader file) throws Exception {
        BufferedReader input = new BufferedReader(file);
        ArrayList<String> output = new ArrayList<String>();

        String tempS;
        while ((tempS = input.readLine()) != null) {
            if (!tempS.isEmpty()) {
                output.add(tempS);
            } else {    //To perserve the blank lines after the paragraph
                String tempArr = output.get(output.size() - 1);
                tempArr += "\n";
                output.set(output.size() - 1, tempArr);   //Add blank line to the last element of output
            }
        }

        return output;
    }

    //      Separate all the tokens in the input file and put them in an array list
    //      Fragmented by whitespace
    private static ArrayList<String> tokenFragment(FileReader file) throws Exception {
        ArrayList<String> output = new ArrayList<String>();
        String token = "";
        boolean addMore = false;

        int tempC;
        while ((tempC = file.read()) != -1) {
            if (Character.isWhitespace((char) tempC)) {
                addMore = true;
            } else {
                if (addMore) {
                    output.add(token);
                    token = "";
                    addMore = false;
                }
            }

            token += (char) tempC;
        }

        output.add(token);
        return output;
    }

    //      Separate all the tokens in the input file and put them in an array list
    //      Fragmented by whitespace
    private static ArrayList<String> charFragment(FileReader file) throws Exception {
        ArrayList<String> output = new ArrayList<String>();

        int tempC;
        while ((tempC = file.read()) != -1) {
//            if (!Character.isWhitespace((char) tempC)) {      //Whitespace is needed for output
            output.add(String.valueOf((char) tempC));
//            }
        }

        return output;
    }
}

// Class used by the patch utility to read commands off the patch file
class Command {
    private char comm;
    private int row;
    private String args;
    //    Adds or deletes an element to/from the input ArrayList
    public static void execCommand(ArrayList<String> input, Command command) {
        if (command.getComm() == 'I') {
            input.add(command.getRow(), command.getArgs());
        } else {
            input.remove(command.getRow() - 1);
        }
    }
    String getArgs() {
        return args;
    }
    char getComm() {
        return comm;
    }
    int getRow() {
        return row;
    }

    Command(char comm, int row, String args) {
        this.comm = comm;
        this.row = row;
        this.args = args;
    }

    @Override
    public String toString() {
        return row + " " + comm + " " + args;
    }



}

