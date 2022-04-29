/* Author: Michael Shoenberger
 * CSCI406: Maze Project
 * NOTE: REQUIRES JGraphT Library to run
 */

import java.util.Scanner;
import java.io.File;
import java.util.Timer;

import org.jgrapht.*;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

/*
 * Class to implement my solution to the Maze Project
 * Utilizes jgrapht Java library to provide graph functionality and algorithm functionality
 */
public class MazeLogic {

	//Member variables for file reading, used to store the initial values from the file into an arraylist
	private File inputFile;
	private Scanner fileScanner;

	//Hold the input file internally so we can reference however we want too
	private int[][] tableJumpLengths;
	private int maxRows;
	private int maxColumns;

	//Graph to be generated as we work through the code
	private DefaultDirectedGraph<String, DefaultEdge> problemGraph;

	/*
	 * Method to conduct all of our work in generating/building the graph itself
	 */
	public MazeLogic() {

		//First we will get the input file and make sure it opens correctly
		this.inputFile = new File("input.txt"); //create a file object
		try { //Allow scanner to hold the file
			this.fileScanner = new Scanner(this.inputFile);
		}catch (Exception e) { //Give an error if an exception is thrown
			System.out.println("Error reading the file");
			System.exit(1);
		}

		readFile(); //With a valid file we can create a copy of it interally

		this.problemGraph = new DefaultDirectedGraph<>(DefaultEdge.class); //Initialize the graph object at this point

		generateVertices(); //Generate vertices with names of their overall location and which level they are associated with
		generateEdges(); //Creates the normal traversal graph portion and then then diagonal graph portion, linking the two then necessary
	}

	/*
	 * Method to read in the file and create a local copy
	 */
	public void readFile() {

		//Get the first two numbers representing the maximum row and columns of the provided trampoline puzzle
		int numRows = this.fileScanner.nextInt();
		this.maxRows = numRows;
		int numColumns = this.fileScanner.nextInt();
		this.maxColumns = numColumns;

		//Initialize the a 2D array to hold the jump distances at each trampoline
		this.tableJumpLengths = new int[numRows][numColumns];

		//Iterate through all of the trampolines
		for(int i = 0; i < numRows; i++) {
			for(int j = 0; j < numColumns; j++) {
				this.tableJumpLengths[i][j] = this.fileScanner.nextInt(); //And store their jump values
			}
		}
	}

	/*
	 * Prints the length array to see if it looks correct, was a helper method for myself
	 * Included for completeness
	 */
	public void printArray() {

		//Print out our 2D array to make sure the jump values are accurate, MUST BE GOOD BEFORE MOVING ON 
		for(int i = 0; i < this.maxRows; i++) {
			for(int j = 0; j < this.maxColumns; j++) {
				System.out.print(this.tableJumpLengths[i][j] + ", ") ;
			}
			System.out.println();
		}
	}

	/*
	 * Generates the vertices to be used in the graph. Will be of the following format:
	 * # N/D 
	 * # represents a number between 0 and 63 to represent the which number the Vertex represents
	 * N/R represents a 1 character representation on if the vertex is in the normal plane or the diagonal plane
	 * Example: 63N represents the end result node in the normal graph at (8,8)
	 */
	public void generateVertices() {

		//Get the total number of trampolines for the given file, generically written so any file would work
		int numberTrampolines = this.maxColumns * this.maxRows;

		String normalPlaneIndicator = "N"; //Normal movement node indicator
		String diagonalPlaneIndicator = "D"; //Diagonal movement vertex indicator

		for(int vertexNumber = 0; vertexNumber < numberTrampolines; vertexNumber++) { //For each of the 64 trampolines

			//FIRST CREATE THE NORMAL VERTICES 0N THROUGH 63N
			String normalVertexName = vertexNumber + normalPlaneIndicator;
			this.problemGraph.addVertex(normalVertexName); //Add the normal direction vertex to the graph

			//NEXT CREATE THE DIAGONAL VERTICES 0D THORUGH 63D
			String diagonalVetexName = vertexNumber + diagonalPlaneIndicator;
			this.problemGraph.addVertex(diagonalVetexName); //Add the diagonal direction vertex to the graph
		}
	}

	/*
	 * Method to build the edges in the graph based on the current location of the node
	 * Must build out the Normal plane and the Diagonal plane, and have the ability to cross over if at negative numbers
	 * Will use many helper methods
	 * NOTE: implementation could be refactored more to minimize repeat code, but this works and flows easily so it is fine enough
	 * Uses the identifiers to build the graph 
	 */
	public void generateEdges() {

		//First we will generate edges for the normal portion of the graph
		generateNormal();
		//Now we generate the edges for the diagonal portions of the graph
		generateDiagonal();
		
		//Now set the end point to both point to each other, doesn't matter if we get there diagonally or normally
		this.problemGraph.addEdge("63N", "63D");
		this.problemGraph.addEdge("63D", "63N");
	}

	/*
	 * Connects vertices in Normal (horizontal and vertical) order based on what are valid targets for them
	 * We must check each of the 4 cardinal directions to see if that movement is valid, as not every movement will have 4 options to jump to
	 * Also must implement a switch over method that will take us to the diagonal case if we hit a negative node
	 */
	public void generateNormal() {

		//ITERATE THROUGH EVERY TRAMPOLINE IN OUR GRAPH
		for(int rowNumber = 0; rowNumber < this.maxRows; rowNumber++) {
			for(int columnNumber = 0; columnNumber < this.maxColumns; columnNumber++) {

				int currentJumpNumber = this.tableJumpLengths[rowNumber][columnNumber]; //how much we need to jump based on the current trampoline
				int currentVertexNumber = rowNumber * this.maxColumns + columnNumber; //Get the current vertexNumber to represent the current trampoline indicator value

				//Check if we are at a negative number, we need to do do switch from normal to diagonal
				if(currentJumpNumber < 0){
					int positiveJumpNumber = -1 * currentJumpNumber;
					switchFromNormalToDiagonal(rowNumber, columnNumber, positiveJumpNumber, currentVertexNumber); //Handles calculating the diagonal options in the negative case
				}else {

					//We create normal jump associations in each of the directions that are valid
					//ALL OF THESE WILL BE OF "N" DESIGNATION AT THIS POINT	
					
					//LOGIC: Just need to check the cardinal directions from the current point to ensure valid jump points. Simple directional math

					//BEGIN ASSIGNING THE 4 DIRECTIONS
					if(rowNumber - currentJumpNumber >= 0) { //UP DIRECTION VALID CHECK
						int upVertexNumber = (rowNumber - currentJumpNumber) * this.maxColumns + columnNumber; //Get the vertexNumber associated with straight up, modify row number up on the graph 
						this.problemGraph.addEdge(currentVertexNumber + "N", upVertexNumber + "N"); 	
					}

					if(rowNumber + currentJumpNumber < this.maxRows) { //DOWN DIRECTION VALID CHECK
						int downVertexNumber = (rowNumber + currentJumpNumber) * this.maxColumns + columnNumber; //Get vertexNumber associated with straight down, add to row Number down
						this.problemGraph.addEdge(currentVertexNumber + "N", downVertexNumber + "N"); 

					}

					if(columnNumber - currentJumpNumber >= 0) { //LEFT DIRECTION VALID CHECK
						int leftVertexNumber = rowNumber * this.maxColumns + (columnNumber - currentJumpNumber);
						this.problemGraph.addEdge(currentVertexNumber + "N", leftVertexNumber + "N");

					}

					if(columnNumber + currentJumpNumber < this.maxColumns) { //RIGHT DIRECTION VALID CHECK
						int rightVertexNumber = rowNumber * this.maxColumns + (columnNumber + currentJumpNumber);
						this.problemGraph.addEdge(currentVertexNumber + "N", rightVertexNumber + "N");

					}
				}

			}
		}}
	
	/*
	 * Method to handle moving from the normal horizontal/vertical movement to our diagonal cases
	 * Should prevent both copies of the negative node from being taken, as at that point we are marking both as seen in any standard algorithm
	 */
	public void switchFromNormalToDiagonal(int currentRowNumber, int currentColumnNumber, int currentJumpNumber, int currentVertexNumber) {
		
		/* If we are here, we need to do diagonal work to check all 4 diagonal directions
		 * NOTE: THE DIAGONAL DIRECTIONS ARE ISCOSCELES TRIANGLES, NO NEED FOR PYTHAGOREAN THEORM OR ANYTHING, JUST MOVE BOTH DIRECTOINS THE PROPER AMOUNT
		 * The indicator N will switch over to D to indicate we are at those nodes at that point
		 */
		
		//LOGIC: Due to isosceles triangles, the movement in both x and y will be the same. Any movement check will need to adjust the x and y movement according to the direction required 

		
		//TOP LEFT CHECK (toward the start trampoline)
		if(currentRowNumber - currentJumpNumber >= 0 && currentColumnNumber - currentJumpNumber >= 0) {
			int topLeftDiagonalNumber = (currentRowNumber - currentJumpNumber) * this.maxColumns + (currentColumnNumber - currentJumpNumber);
			this.problemGraph.addEdge(currentVertexNumber + "N", topLeftDiagonalNumber + "D");
		}
		
		//TOP RIGHT CHECK
		if(currentRowNumber - currentJumpNumber >= 0 && currentColumnNumber + currentJumpNumber < this.maxColumns) {
			int topRightDiagonalNumber = (currentRowNumber - currentJumpNumber) * this.maxColumns + (currentColumnNumber + currentJumpNumber);
			this.problemGraph.addEdge(currentVertexNumber + "N", topRightDiagonalNumber + "D");
		}
		
		//BOTTOM LEFT CHECK
		if(currentRowNumber + currentJumpNumber < this.maxRows && currentColumnNumber - currentJumpNumber >= 0) {
			int downLeftDiagonalNumber = (currentRowNumber + currentJumpNumber) * this.maxColumns + (currentColumnNumber - currentJumpNumber);
			this.problemGraph.addEdge(currentVertexNumber + "N", downLeftDiagonalNumber + "D");
		}
		
		//BOTTOM RIGHT CHECK (toward the finish trampoline)
		if(currentRowNumber + currentJumpNumber < this.maxRows && currentColumnNumber + currentJumpNumber < this.maxColumns) {
			int bottomRightDiagonalNumber = (currentRowNumber + currentJumpNumber) * this.maxColumns + (currentColumnNumber + currentJumpNumber);
			this.problemGraph.addEdge(currentVertexNumber + "N", bottomRightDiagonalNumber + "D");
		}
		
	}
	
	/*
	 * Connects vertices in Normal (horizontal and vertical) order based on what are valid targets for them
	 * We must check each of the 4 cardinal directions to see if that movement is valid, as not every movement will have 4 options to jump to
	 * Also must implement a switch over method that will take us to the diagonal case if we hit a negative node
	 */
	public void generateDiagonal() {

		//ITERATE THROUGH EVERY TRAMPOLINE IN OUR GRAPH
		for(int rowNumber = 0; rowNumber < this.maxRows; rowNumber++) {
			for(int columnNumber = 0; columnNumber < this.maxColumns; columnNumber++) {

				int currentJumpNumber = this.tableJumpLengths[rowNumber][columnNumber]; //how much we need to jump based on the current trampoline
				int currentVertexNumber = rowNumber * this.maxColumns + columnNumber; //Get the current vertexNumber to represent the current trampoline

				//Check if we are at a negative number, we need to do do switch from normal to diagonal cases
				if(currentJumpNumber < 0){
					int positiveJumpNumber = -1 * currentJumpNumber;
					switchFromDiagonalToNormal(rowNumber, columnNumber, positiveJumpNumber, currentVertexNumber);
				}else {
					
					//IF WE ARE HERE, WE KNOW THAT WE ARE STILL IN THE DIAGONAL CASE, CHECK ALL 4 DIAGONAL OPTIONS
					
					//TOP LEFT CHECK
					if(rowNumber - currentJumpNumber >= 0 && columnNumber - currentJumpNumber >= 0) {
						int topLeftDiagonalNumber = (rowNumber - currentJumpNumber) * this.maxColumns + (columnNumber - currentJumpNumber);
						this.problemGraph.addEdge(currentVertexNumber + "D", topLeftDiagonalNumber + "D");
					}
					
					//TOP RIGHT CHECK
					if(rowNumber - currentJumpNumber >= 0 && columnNumber + currentJumpNumber < this.maxColumns) {
						int topRightDiagonalNumber = (rowNumber - currentJumpNumber) * this.maxColumns + (columnNumber + currentJumpNumber);
						this.problemGraph.addEdge(currentVertexNumber + "D", topRightDiagonalNumber + "D");
					}
					
					//BOTTOM LEFT CHECK
					if(rowNumber + currentJumpNumber < this.maxRows && columnNumber - currentJumpNumber >= 0) {
						int downLeftDiagonalNumber = (rowNumber + currentJumpNumber) * this.maxColumns + (columnNumber - currentJumpNumber);
						this.problemGraph.addEdge(currentVertexNumber + "D", downLeftDiagonalNumber + "D");
					}
					
					//BOTTOM RIGHT CHECK
					if(rowNumber + currentJumpNumber < this.maxRows && columnNumber + currentJumpNumber < this.maxColumns) {
						int bottomRightDiagonalNumber = (rowNumber + currentJumpNumber) * this.maxColumns + (columnNumber + currentJumpNumber);
						this.problemGraph.addEdge(currentVertexNumber + "D", bottomRightDiagonalNumber + "D");
					}

				}

			}
		}}
	
	/*
	 * Method to handle moving from the diagonal movement to our normal horizontal/vertical movement behavior
	 * Should prevent both copies of the negative node from being taken, as at that point we are marking both as seen in any standard algorithm
	 */
	public void switchFromDiagonalToNormal(int currentRowNumber, int currentColumnNumber, int currentJumpNumber, int currentVertexNumber) {
		
		//If we are here, we need to do normal work to check all 4 cardinal directions
		//ALL OF THESE WILL BE OF "N" DESIGNATION AT THIS POINT	AS SWITCHING TO N(ormal) FROM D(iagonal)

		//BEGIN ASSIGNING THE 4 DIRECTIONS
		if(currentRowNumber - currentJumpNumber >= 0) { //UP DIRECTION VALID CHECK
			int upVertexNumber = (currentRowNumber - currentJumpNumber) * this.maxColumns + currentColumnNumber; //Get the vertexNumber associated with straight up, modify row number up on the graph 
			this.problemGraph.addEdge(currentVertexNumber + "D", upVertexNumber + "N"); 	
		}

		if(currentRowNumber + currentJumpNumber < this.maxRows) { //DOWN DIRECTION VALID CHECK
			int downVertexNumber = (currentRowNumber + currentJumpNumber) * this.maxColumns + currentColumnNumber; //Get vertexNumber associated with straight down, add to row Number down
			this.problemGraph.addEdge(currentVertexNumber + "D", downVertexNumber + "N"); 

		}

		if(currentColumnNumber - currentJumpNumber >= 0) { //LEFT DIRECTION VALID CHECK
			int leftVertexNumber = currentRowNumber * this.maxColumns + (currentColumnNumber - currentJumpNumber);
			this.problemGraph.addEdge(currentVertexNumber + "D", leftVertexNumber + "N");

		}

		if(currentColumnNumber + currentJumpNumber < this.maxColumns) { //RIGHT DIRECTION VALID CHECK
			int rightVertexNumber = currentRowNumber * this.maxColumns + (currentColumnNumber + currentJumpNumber);			
			this.problemGraph.addEdge(currentVertexNumber + "D", rightVertexNumber + "N");
		}
	}
	
	/*
	 * Simple method to return the graph for use in the Algorithm objects that jgrapht provides
	 */
	public DefaultDirectedGraph<String, DefaultEdge> getGraph(){
		return this.problemGraph;
	}
	
	/*
	 * Finally, we must convert from the output of jgrapht to the desired output format for the project
	 * My design decision of #N/R makes this process a little more intensive but not too much more difficult. 
	 * Need to remove items from total output as it normally looks as follows from jgrapht:
	 * [(0N : 32N), (32N : 59D), (59D : 31D), (31D : 4D), (4D : 32D), (32D : 8N), (8N : 11N), (11N : 43N), (43N : 47N), (47N : 54D), (54D : 0D), (0D : 36D), (36D : 22D), (22D : 57D), (57D : 59N), (59N : 63N)]
	 * Notice how it shows new vertices on the left of each pair, and that it follows my convention. Just remove the right hand side AND allow for us to 
	 */
	public static void convertPathToSolution(String outputPath) {
		
		outputPath = outputPath.substring(1, outputPath.length() - 1); //remove the 2 square brackets from jgrapht breadth search
		outputPath = outputPath.replace("(", ""); //Remove the ( character 
		outputPath = outputPath.replace(")", ""); //Remove the ) character
		outputPath = outputPath.replace("N", "");//Remove the N character
		outputPath = outputPath.replace("D", "");//Remove the D character

		String[] splitString = outputPath.split(", ");
		String finalPath = "";
		
		//Now iterate over each instance and get the first number, and convert that number to our points
		for(int i = 0; i < splitString.length; i++) {
			
			String[] removeColon = splitString[i].split(":");
			int stringSize = removeColon[0].length();
			int vertexIndex = Integer.parseInt(removeColon[0].substring(0, stringSize - 1));
						
			int rowIndex = vertexIndex / 8 + 1;
			int columnIndex = vertexIndex % 8 + 1;
			finalPath += "(" + rowIndex + "," + columnIndex + ") ";

			//If we are going from an end node to end node, then we already are there, just fill in the final interpretation
			if(i == splitString.length - 1 && vertexIndex != 63) {				
				finalPath += "(8,8)";
			}
		}
		System.out.println(finalPath);
	}

	/*
	 * Main method to run the program, is small as the work is done in methods, not in main
	 */
	public static void main(String[] args) {
		MazeLogic mazeGraph = new MazeLogic(); //Create a new object of the MazeLogic class to generate the graph using the 2-Directional plane approach
		
		BFSShortestPath<String, DefaultEdge> BFSAlgo = new BFSShortestPath<String, DefaultEdge>(mazeGraph.getGraph()); //Analyze the graph created in a BFS shortest path algorithm provided by JGraphT library
		
		String outputPath = BFSAlgo.getPath("0N", "63D").toString(); //Get the output from the algorithm for the path from start to end
		convertPathToSolution(outputPath); //Interpret the output into the correct nomenclature
	}
}
