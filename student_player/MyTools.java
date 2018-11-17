package student_player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import coordinates.Coord;
import coordinates.Coordinates;
import tablut.TablutBoardState;
import tablut.TablutBoardState.Piece;
import tablut.TablutMove;

public class MyTools {
    private static final int MAX_DEPTH = 3; 
    private static final int MINF = -10000000; 
    private static final int PINF =  10000000;
    
    //stores a board state, its parent, the move taken to reach it, and the value of the board state.
    //we need information regarding the best reachable board state x moves ahead, the next move required to reach it
    //the value of alpha and beta
    private static class Node{

    	TablutBoardState bs;
    	int depth;
    	int alpha = MINF;
    	int beta = PINF;
    	
    	Node parent;
    	TablutMove move; //move chosen to get to the state
    	
    	public Node(TablutBoardState bs, int depth, int alpha, int beta){
    		this.bs    = bs;
    		this.depth = depth;
    		this.alpha = alpha;
    		this.beta  = beta;
    	}
    	
    }
 
    private static Node getRootState(Node state){
    	Node rootState = state;
    	while(rootState.parent !=null) rootState = rootState.parent;
    	return rootState;
    }
    
    //Heuristic functions
    private static int getCaptureScore(Node currentState, Node rootState){
    	
    	int opponent = rootState.bs.getOpponent();
    	int myPlayer = rootState.bs.getTurnNumber();
    	int numberOfOpponentPieces = currentState.bs.getNumberPlayerPieces(opponent);
    	int numberOfMyPlayerPieces = currentState.bs.getNumberPlayerPieces(myPlayer);
        return (numberOfMyPlayerPieces-numberOfOpponentPieces);
    }
       
    private static int getDistanceScore(Node currentState){ 	
    	Coord newKingPos  = currentState.bs.getKingPosition();
        int distance = Coordinates.distanceToClosestCorner(newKingPos)*(-1);
        return distance;
    }
    
    //aims to have the king not surrounded by any pieces.
    private static int getKingSurroundScore(Node currentState){
    	Coord kingPos  = currentState.bs.getKingPosition();
    	List<Coord> kingNeighbors = Coordinates.getNeighbors(kingPos);
    	int kingSurroundScore = 0;
    	
    	//if the king is on an edge, we can't evaluate its surroundings.
    	if(Coordinates.isCenterOrNeighborCenter(kingPos)){
    		kingSurroundScore += 2;
    	}
	    	try{
	    		int count = 0;
		    	for(Coord neighborPos : kingNeighbors){
		    		if (count == 2) break;
		    		//empty front
		    		if(currentState.bs.coordIsEmpty(neighborPos)){
		    			if(currentState.bs.coordIsEmpty(Coordinates.getSandwichCoord(neighborPos, kingPos))){
		    				kingSurroundScore += 2; //not sandwiched
		    			}
		    			else if(currentState.bs.isOpponentPieceAt(Coordinates.getSandwichCoord(neighborPos, kingPos))){
		    				kingSurroundScore -= 2; //one opponent piece next to you. dangerous!
		    			}
		    		} 
		    		//opponent in front
		    		else if(currentState.bs.isOpponentPieceAt(neighborPos)){
		    			if(currentState.bs.coordIsEmpty(Coordinates.getSandwichCoord(neighborPos, kingPos))){
		    				kingSurroundScore -= 2; //one opponent piece next to you. dangerous!
		    			}
		    			else kingSurroundScore -= 1; //opponent piece sandwiched with another piece
		    		}
		    		//own piece in front and sandwiching
		    		else if(!currentState.bs.coordIsEmpty(Coordinates.getSandwichCoord(neighborPos, kingPos))){
		    			kingSurroundScore -= 1; //sandwiched by your piece with another piece. king wants to be free.
		    		}
		    		count++;
		    	}
	    		} catch(Coordinates.CoordinateDoesNotExistException e){
	    			//System.out.println("Cannot find sandwich. kingSurroundScore not considered");
	    	}
    	
    	return kingSurroundScore;
    }
    
    //give more value to certain spots on the board. this helps muscovites
    private static int getKeyAreasScore(Node currentState){

    	List<Coord> keyAreas = new ArrayList<Coord>();
    	keyAreas.add(Coordinates.get(1, 2));
    	keyAreas.add(Coordinates.get(2, 1));
    	keyAreas.add(Coordinates.get(1, 6));
    	keyAreas.add(Coordinates.get(6, 1));
    	keyAreas.add(Coordinates.get(2, 7));
    	keyAreas.add(Coordinates.get(7, 2));
    	keyAreas.add(Coordinates.get(6, 7));
    	keyAreas.add(Coordinates.get(7, 6));
    	

    	int keyAreasScore = 0;
    	for(Coord keyArea : keyAreas){
    		if(!(currentState.bs.getPieceAt(keyArea) == Piece.EMPTY)) keyAreasScore +=1;
    	}
    	return keyAreasScore;
    	
    }

    private static Node abSearch(Node currentState){

    	boolean isMaxPlayer = currentState.depth % 2 == 0;
    	//Terminal node
        //once a terminal state is reached, evaluate the board state
        //store this score into the node and return the node
        if(currentState.depth == MAX_DEPTH){
        	Node rootState = getRootState(currentState);
        	int terminalValue = 0;
        	
        	int captureScore = getCaptureScore(currentState, rootState)*100000;
        	int distanceScore = getDistanceScore(currentState)*10000;
        	int keyAreasScore = getKeyAreasScore(currentState)*1000;
        	int kingSurroundScore = 0;
        	List<Coord> corners = Coordinates.getCorners();
        	List<Coord> edges = new ArrayList<Coord>();
        	int i = 0;
        	int edgeScore = 0;
        	for (Coord corner : corners){
        		if (i % 2 == 0){
        			for(Coord otherCorner : corners){
        				List<Coord> edge = corner.getCoordsBetween(otherCorner);
            			edges.addAll(corner.getCoordsBetween(otherCorner));
        				if (edge.contains(currentState.bs.getPlayerPieceCoordinates())){
        					edgeScore += 1;
        				}
        			
            			
            		}
        		}
        		i++;
        	}
        	if(!edges.contains(currentState.bs.getKingPosition())) kingSurroundScore += getKingSurroundScore(currentState)*1000;
        	
        	
        	if(rootState.bs.getTurnPlayer() == TablutBoardState.SWEDE) {
        		terminalValue = captureScore + distanceScore + kingSurroundScore;
        	}else{
        		terminalValue = captureScore - distanceScore + keyAreasScore;
        	}
        	
        	currentState.alpha = terminalValue;
        	currentState.beta  = terminalValue;
        }
        else {	//if depth < 4 then evaluate all successors of current state.

	    	List<TablutMove> options = currentState.bs.getAllLegalMoves();

	    	for (TablutMove move : options) {

	    		TablutBoardState cloneBS = (TablutBoardState) currentState.bs.clone();
	            cloneBS.processMove(move);
	        	
	            //Create node for a successor of currentState and set its parent, move, and a/b values. 
	            Node successorState = new Node(cloneBS, currentState.depth+1, currentState.alpha, currentState.beta);
	            successorState.parent = currentState;
	            successorState.move   = move;
            
	            //if the move is a winning move, update to max score immediately. there are no children.
	        	if (successorState.bs.getWinner() == getRootState(successorState).bs.getOpponent()) {
	        		successorState.beta = MINF;
	        		successorState.alpha = MINF;
	        		successorState.move   = move;
	        	}
	        	else if (successorState.bs.getWinner() == getRootState(successorState).bs.getTurnPlayer()) {
	        		successorState.alpha += PINF; 
	        		successorState.beta += PINF;
	        		successorState.move   = move;
	        	}
	        	else successorState = abSearch(successorState); //returns successorState with updated a/b values
	            
	            //After recursion: update alpha/beta for max/min player of the current state
	            //MAX PLAYER
	            //at depth 0 and 2, find the greatest value of all successors (update alpha)
	            //The root node (currentState) updates its move if alpha improves. 
	            if(isMaxPlayer && successorState.beta > currentState.alpha) {
	            	currentState.alpha = successorState.beta;
	            	//update best move at root when alpha improves.
	            	if(currentState.depth == 0) currentState.move = successorState.move; 
	            }
	            
	            //MIN PLAYER
	            //at depth 1 and 3, find the lowest value of all successors (update beta)
	            else if(!isMaxPlayer && successorState.alpha < currentState.beta) currentState.beta = successorState.alpha;

	            //PRUNING
	            //The AI avoids a move that gives the opponent a better option than other moves that the AI has found.
	            if (currentState.alpha > currentState.beta) {
	            	break;
	            }
	    	}
        }
        
        return currentState;
    }
    
    public static TablutMove chooseMove(TablutBoardState bs){
    	Node startState = new Node(bs, 0, MINF, PINF);

    	TablutMove moveChosen = abSearch(startState).move;
    	return moveChosen;
    }
}
