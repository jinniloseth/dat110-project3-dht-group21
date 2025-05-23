package no.hvl.dat110.util;

/**
 * @author tdoy
 * dat110 - project 3
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import no.hvl.dat110.middleware.Message;
import no.hvl.dat110.rpc.interfaces.NodeInterface;

public class FileManager {

	private static final Logger logger = LogManager.getLogger(FileManager.class);

	private BigInteger[] replicafiles; // array stores replicated files for distribution to matching nodes
	private int numReplicas; // let's assume each node manages nfiles (5 for now) - can be changed from the
								// constructor
	private NodeInterface chordnode;
	private String filepath; // absolute filepath
	private String filename; // only filename without path and extension
	private BigInteger hash;
	private byte[] bytesOfFile;
	private String sizeOfByte;

	private Set<Message> activeNodesforFile = null;

	public FileManager(NodeInterface chordnode) throws RemoteException {
		this.chordnode = chordnode;
	}

	public FileManager(NodeInterface chordnode, int N) throws RemoteException {
		this.numReplicas = N;
		replicafiles = new BigInteger[N];
		this.chordnode = chordnode;
	}

	public FileManager(NodeInterface chordnode, String filepath, int N) throws RemoteException {
		this.filepath = filepath;
		this.numReplicas = N;
		replicafiles = new BigInteger[N];
		this.chordnode = chordnode;
	}

	
	//replikaene opprettes korrekt 
	public void createReplicaFiles() {
		
		// 1. set a loop where size = numReplicas

		// 2. replicate by adding the index to filename

		// 3. hash the replica

		// 4. store the hash in the replicafiles array.
		for (int i = 0; i < numReplicas; i++) {
			String replicaName = filename + i;
			replicafiles[i] = Hash.hashOf(replicaName);
		}
	}

	/**
	 * 
	 * @param bytesOfFile
	 * @throws RemoteException
	 */
	
	// Gjør at replikaene distribueres til riktige noder
	// altså er målet med metoden å ta en fil, lage flere kopier (replikaer) av den, 
	// og distribuere disse kopiene til forskjellige noder i Chord-ringen
	public int distributeReplicastoPeers() throws RemoteException {
	// først lages replikaene. 
		createReplicaFiles();

		// randomly appoint the primary server to this file replicas
		Random rnd = new Random();
		int index = rnd.nextInt(numReplicas);

		int counter = 0;
		
		//looper gjennom replikaene og finner riktig node. For hver replika finner vi den 
		//nærmeste noden i Chord-ringen som kan lagre denne
		for(int i = 0; i< numReplicas; i++) {
			BigInteger replicaHash= replicafiles[i];
			NodeInterface successor= chordnode.findSuccessor(replicaHash);
			if(successor != null) {
				//noden legger til hashverdien i sin liste over lagrede nøkler
				successor.addKey(replicaHash);
				
				//bestemmer om noden skal være primær. Dersom counter er lik det tilfeldige
				//tallet index, blir denne noden primær
				boolean isPrimary = (counter == index);
				successor.saveFileContent(filename, replicaHash, bytesOfFile, isPrimary);
				//for hver gang en replika lagres, så øker vi telleren. 
				counter++; 
			}
		}

		// Task1: Given a filename, make replicas and distribute them to all active
		// peers such that: pred < replica <= peer

		// Task2: assign a replica as the primary for this file. Hint, see the slide
		// (project 3) on Canvas

		// create replicas of the filename

		// iterate over the replicas

		// for each replica, find its successor (peer/node) by performing
		// findSuccessor(replica)

		// call the addKey on the successor and add the replica

		// implement a logic to decide if this successor should be assigned as the
		// primary for the file

		// call the saveFileContent() on the successor and set isPrimary=true if logic
		// above is true otherwise set isPrimary=false

		// increment counter
		return counter;
	}

	/**
	 * 
	 * @param filename
	 * @return list of active nodes having the replicas of this file
	 * @throws RemoteException
	 */
	
	// Finner alle noder (peers) i et Chord system som har en kopi av en gitt fil
	//den returnerer da en liste med informasjon om disse nodene. 
	public Set<Message> requestActiveNodesForFile(String filename) throws RemoteException {

		this.filename = filename;
		
		//initialiserer en tom liste som skal lagre informasjonen om alle noder som
		// har en kopi av filen.
		activeNodesforFile = new HashSet<Message>();
		
		createReplicaFiles();
		
		// går gjennom hver hash og lagrer den i variablen replica
		// replicafiles er en liste med hash-verdier for fil-replikaene
		for(int i = 0; i < replicafiles.length; i++) {
			BigInteger replica= replicafiles[i];
			
			// finn successor for hver hash.
			// hver node i chord er ansvarlig for hash-verdier
			// findSuccessor(replica) finner noden som er ansvarlig for denne hash-verdien
			// denne noden har en kopi av filen eller kan lagre den. 
			NodeInterface successor = chordnode.findSuccessor(replica);
			
			// sjekker om successor ble funnet
			if(successor != null) {
				//henter metadata om filen fra successornoden
				Message metadata= successor.getFilesMetadata(replica);
				// hvis metadata finnes, lagres det i activenodeforfile
				//altså vet vi nå hvilken node som holder en kopi av filen.
				if(metadata != null) {
					//lagrer metadataen i listen aktivnodeforfile
				activeNodesforFile.add(metadata);
				}
			}
		}

		// Task: Given a filename, find all the peers that hold a copy of this file

		// generate the N replicas from the filename by calling createReplicaFiles()

		// iterate over the replicas of the file

		// for each replica, do findSuccessor(replica) that returns successor s.

		// get the metadata (Message) of the replica from the successor (i.e., active
		// peer) of the file

		// save the metadata in the set activeNodesforFile.

		
		//returnerer settet med informasjon om noder som har filen. 
		return activeNodesforFile;
	}

	/**
	 * Find the primary server - Remote-Write Protocol
	 * 
	 * @return
	 */
	public NodeInterface findPrimaryOfItem() {

		// Task: Given all the active peers of a file (activeNodesforFile()), find which
		// is holding the primary copy

		// iterate over the activeNodesforFile

		// for each active peer (saved as Message)

		// use the primaryServer boolean variable contained in the Message class to
		// check if it is the primary or not

		// return the primary when found (i.e., use Util.getProcessStub to get the stub
		// and return it)

		for(int i= 0; i< activeNodesforFile.size(); i++) {
			Message msg= (Message) activeNodesforFile.toArray()[i];
			
			if(msg.isPrimaryServer()) {
				return Util.getProcessStub(msg.getNodeName(), msg.getPort());
			}
		}
		
		return null;
	}

	/**
	 * Read the content of a file and return the bytes
	 * 
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public void readFile() throws IOException, NoSuchAlgorithmException {

		File f = new File(filepath);

		byte[] bytesOfFile = new byte[(int) f.length()];

		FileInputStream fis = new FileInputStream(f);

		fis.read(bytesOfFile);
		fis.close();

		// set the values
		filename = f.getName().replace(".txt", "");
		hash = Hash.hashOf(filename);
		this.bytesOfFile = bytesOfFile;
		double size = (double) bytesOfFile.length / 1000;
		NumberFormat nf = new DecimalFormat();
		nf.setMaximumFractionDigits(3);
		sizeOfByte = nf.format(size);

		logger.info("filename=" + filename + " size=" + sizeOfByte);

	}

	public void printActivePeers() {

		activeNodesforFile.forEach(m -> {
			String peer = m.getNodeName();
			String id = m.getNodeID().toString();
			String name = m.getNameOfFile();
			String hash = m.getHashOfFile().toString();
			int size = m.getBytesOfFile().length;

			logger.info(
					peer + ": ID = " + id + " | filename = " + name + " | HashOfFile = " + hash + " | size =" + size);

		});
	}

	/**
	 * @return the numReplicas
	 */
	public int getNumReplicas() {
		return numReplicas;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * @return the hash
	 */
	public BigInteger getHash() {
		return hash;
	}

	/**
	 * @param hash the hash to set
	 */
	public void setHash(BigInteger hash) {
		this.hash = hash;
	}

	/**
	 * @return the bytesOfFile
	 */
	public byte[] getBytesOfFile() {
		return bytesOfFile;
	}

	/**
	 * @param bytesOfFile the bytesOfFile to set
	 */
	public void setBytesOfFile(byte[] bytesOfFile) {
		this.bytesOfFile = bytesOfFile;
	}

	/**
	 * @return the size
	 */
	public String getSizeOfByte() {
		return sizeOfByte;
	}

	/**
	 * @param size the size to set
	 */
	public void setSizeOfByte(String sizeOfByte) {
		this.sizeOfByte = sizeOfByte;
	}

	/**
	 * @return the chordnode
	 */
	public NodeInterface getChordnode() {
		return chordnode;
	}

	/**
	 * @return the activeNodesforFile
	 */
	public Set<Message> getActiveNodesforFile() {
		return activeNodesforFile;
	}

	/**
	 * @return the replicafiles
	 */
	public BigInteger[] getReplicafiles() {
		return replicafiles;
	}

	/**
	 * @param filepath the filepath to set
	 */
	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}
}
