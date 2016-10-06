import java.util.*;
import java.io.*;

/**
 *  Name: Lucas Saladin
 *  Course: CIS 421 Artificail Intelligence
 *  Assignment: 2
 *  Due:  10/05/2016
 */

public class NQueens {
    public static void main(String[] args) {    
        int trials = 25;
        PrintStream log = initPrintStream(new File("results.txt"));
        for (int i=0; i<trials; i++) run(log);
    }
    
    /** 
     * Runs an iteration of reality for the humble population of NQueens. 
     * @param log The log printstream to write to
     */
    public static void run(PrintStream log) {
        int n = 12;
        int maxGen = 1000;
        
        //This is a new seed
        Random r = new Random();
        ArrayList<Geno> population = newPopulation(n, r);
        
        //Check if there's a winner in the initial population, aka generation 0
        boolean winner = handleWinner(population, 0, log);
        
        if(!winner) {
            //For 1000 generations we have passed down this sacred algorithm...
            for (int gen = 1; gen <= maxGen; gen++) {
                //do crossover, mutate some babies, select survivors from all:
                ArrayList<Geno> babies = crossover(population, r);
                mutate(babies, r);
                population = selectSurvivors(population, babies);
                // if a winner has been found, break:
                winner = handleWinner(population, gen, log);
                if (winner) break;
                
                //If this is the last gen, we will never succeed. Print its #:
                if (gen==maxGen) log.println(maxGen);
            }
        }
        log.flush();
    }
    
    /**
     * Does crossover on the given population. Uses tournament selection to 
     * pick 10% of the population to be parents. The crossover point is 
     * randomly selected.
     * @param pop The population to breed
     * @param r The random generator to use
     * @return the fresh crop of babies
     */
    public static ArrayList<Geno> crossover(ArrayList<Geno> pop, Random r) {
        //choose who all is getting paired:
        ArrayList<Geno> allBabies = new ArrayList<>();
        ArrayList<Geno[]> spouses = getSpouses(pop, r);
        for (Geno[] c : spouses) 
            allBabies.addAll(mate(c[0], c[1], r));

        return allBabies;
    }
    
    /**
     * Picks pairs of genomes in the population to mate. Uses tournament 
     * selection, without replacement, to pick this. The code is bad here.
     * @param pop The population to pick spouses for
     * @param r The random generator to use
     * @return A list of pairs of indecies which refer to two spouses
     */
    public static ArrayList<Geno[]> getSpouses(ArrayList<Geno> pop, Random r) {
        //Three tournament members - could be a param
        int members = 3; 
        
        //mating pool
        ArrayList<Geno> pool = new ArrayList<>(); 
        //list of indecies into pop. Remove a random index to pick w/o rplcemnt
        ArrayList<Integer> indecies = new ArrayList<>();
        for (int i=0; i<pop.size(); i++) indecies.add(i); 
        //Tournament selection: pick three randomly, most fit goes into pool
        for (int i=0; i<pop.size()/10; i++) {
            //populate with 3 random contestants and their conflicts
            ArrayList<Geno> contestants = new ArrayList<>();
            for (int j=0; j<members; j++) {
                contestants.add(                 //pick a tourneyman
                    pop.get(                     //from the population*
                        indecies.remove(         //with a so-far unseen index
                            r.nextInt(           //which is random
                                indecies.size()  //from the whol set of unseens
                            )
                        )
                    )
                );
                //*for with replacement, finish like this:r.nextInt(pop.size())
            }
            Collections.sort(contestants);
            pool.add(contestants.get(members-1));
        }
        //Could sort pool here to pick power couples

        //Pair the pool off. Should always have N members
        ArrayList<Geno[]> spouses = new ArrayList<>();
        for (int i=0; i<pool.size(); i+=2)
            spouses.add(new Geno[]{pool.get(i), pool.get(i+1)});
        return spouses;
    }
    
    /**
     * When a mommy and a daddy love each other very much, they do crossover
     * on a randomly selected crossover point, then return the baby. Here, and
     * in the subroutine fillVoids(), I represent genotypes as integer arrays
     * rather than Geno objects. This is an artifact from before I refactored
     * to include class Geno. But we don't need the sorting functionality of 
     * Genos, so they can be arrays until they get returned.
     * @param mom The mommy genotype
     * @param dad The daddy genotype
     * @param r The random generator to use
     * @return The resulting baby genotype
     */
    private static ArrayList<Geno> mate(Geno mother, Geno father, Random r) {
        ArrayList<Geno> babies = new ArrayList<>();
        int[] mom = mother.getGeno();
        int[] dad = father.getGeno();
        //get a random cross point. Cross ::= an int x | genotype splits into 
        //two halves, with index x on the right side. The max cross is len-1,
        //which copies all but one index. However, that results in duplicates
        int cross = r.nextInt(mom.length-1)+1;
        //Make two babies. One is a momma'ss boy and the other a daddy's girl
        int[] mommas = new int[mom.length];
        int[] daddys = new int[mom.length];
        //momma's boy gets momma's 1st half, daddy's girl gets daddy's 1st half
        for (int i=0; i<cross; i++) {
            mommas[i] = mom[i];
            daddys[i] = dad[i];
        }
        //momma's boy fills the voids with dad, daddy's girl does with mom
        fillVoids(mommas, dad, cross);
        fillVoids(daddys, mom, cross);

        //Return the babies
        babies.add(new Geno(mommas));
        babies.add(new Geno(daddys));
        return babies;
    }
    
    /**
     * Uses the values in a parent genotype to fill in the end of a child after
     * filling in the first half already.
     * @param child The child's unfinished genotype, as an integer array
     * @param parent The parent's genotype, as an integer array
     * @param firstEmpty The first empty index in child. All indecies to the 
     *                   right are also empty
     */
    private static void fillVoids(int[] child, int[] parent, int firstEmpty) {
        //Get the child's values into a list
        ArrayList<Integer> childList = new ArrayList<>();
        for (int i=0; i<firstEmpty; i++) childList.add(child[i]);
        
        //Iterate over parent, add to end of the child any values not in child
        for (int i=0; i<parent.length; i++) {
            if (!childList.contains(parent[i])) {
                child[firstEmpty] = parent[i];
                firstEmpty++; 
            }
        }
    }
    
    /**
     * Mutates some of the babies. Any given baby has a 10% chance of being 
     * mutated. Mutation simply swaps queens from two random columns.
     * @param babies The babies to mutate
     * @param r The random generator to use
     */
    public static void mutate(ArrayList<Geno> babies, Random r) {
        int[] baby = null;
        int[] swap = null;
        
        int temp = 0;
        for (int i=0; i<babies.size(); i++) {
            if (r.nextDouble()<.1) { //Then this baby is a mutant freak
                baby = babies.get(i).getGeno();
                //Get two random indecies and swap their contents
                swap = new int[]{
                    r.nextInt(baby.length), 
                    r.nextInt(baby.length)
                };
                //now do that swap thing we all know and love
                temp = baby[swap[0]]; //Now overwrite baby[swap[0]]
                baby[swap[0]] = baby[swap[1]];
                baby[swap[1]] = temp;
            }            
        }
    }
    
    /**
     * Combines babies into the population, then kills the least fit to keep 
     * the population the same size. Just like in real life.
     * @param pop The current population
     * @param babies The babies to add to the population
     * @return the new population of the same size as before
     */
    public static ArrayList<Geno> selectSurvivors(ArrayList<Geno> pop, 
                                                   ArrayList<Geno> babies) {
        int targetSize = pop.size();
        pop.addAll(babies); //Babies are now at the end
        cull(pop, targetSize);        
        return pop;
    }
    
    /**
     * Cuts the population down to a given size. The least fit are killed.
     * @param pop The population
     * @param size The target size for the population
     */
    private static void cull(ArrayList<Geno> pop, int size) {
        //sort it and kill from the back until target size is reached
        Collections.sort(pop);
        for (int i=pop.size()-1; i>=size; i--) pop.remove(i);
    }
    
    /**
     * Looks for a winner in the population. We only care about one winner, 
     * so we find the first one, print it to log, and get out
     * @param pop The population to check
     * @param gen The current generation
     * @param log The log file to write to when a winner is found
     * @return Whether or not we found a winner
     */
    public static boolean handleWinner(ArrayList<Geno> pop, int gen, 
                                  PrintStream log) {
        for (Geno x : pop) {
            if (x.getConflicts()==0) {
                log.print(gen + " ");
                x.printGenome(log);
                log.println();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Initializes a PrintStream, catching exceptions.
     * @param f The file over which to initialize a PrintStream
     * @return the PrintStream
     */
    public static PrintStream initPrintStream(File f) {
        PrintStream log = null;
        try {
            log = new PrintStream(f);
        } catch (FileNotFoundException e) {
            System.out.println("File is bad.");
            System.exit(1);
        }
        return log;
    }
    
    /**
     * Starts out the population with n*10 random genotypes.
     * @param pop the population container
     * @param n the n value for n-queens
     * @param r the random object to use
     * @return the population
     */
    public static ArrayList<Geno> newPopulation(int n, Random r) {
        ArrayList<Geno> population = new ArrayList<>();
        for (int i=0; i<n*10; i++) {
            population.add(new Geno(n, r));
        }
        return population;
    }
    
    public static class Geno implements Comparable<Geno>{
        /**
         * An object to represent a board configuration. This was added at the
         * end of the project to fix some issues and clean up the code, so some
         * tasteful artifacts of the original implementation remain in the code.
         * Using new objects rather than simply integer arrays allowed me to
         * sort populations, which turned out to be extremely helpful.
         */
        private int[] geno;
        
        /**
         * Constructs a geno based on an input array.
         * @param geno the array genotype
         */
        public Geno(int[] geno) {
            this.geno = geno;
        }
        
        /**
         * Gets a random genotype for initializing the population.
         * @param n the n value for n-queens
         * @param r the random object to use
         */
        public Geno(int n, Random r) {
            int[] g = new int[n];
            ArrayList<Integer> positions = new ArrayList<>();
            //populate positions with all possible y-locations for queens
            for (int i=0; i<n; i++) positions.add(i);
            //Populate the genome with random y-positions
            for (int i=0; i<n; i++)
                g[i] = positions.remove(r.nextInt(positions.size()));
            
            geno = g;
        }
        
        /** 
        * Determines the number of conflicts in this board configuration.
        * @param gen the board configuration genome
        * @return the number of conflicts
        */
        public int getConflicts() {
            int conflicts = 0;
            //check diagonals to the left of each queen. 
            for (int atkCol=0; atkCol<geno.length; atkCol++) {
                for (int defCol=atkCol-1, d=1; defCol>=0; defCol--, d++) {
                    if (geno[defCol]==geno[atkCol]+d
                    ||geno[defCol]==geno[atkCol]-d) conflicts++;
                }
            }
            return conflicts;
        }
        
        /**
         * @return the internal array, which may be a boo-boo overall
         */
        public int[] getGeno() {
            return this.geno;
        }
        
        /** 
         * @param index the index to get
         * @return the value at the given index in the genome
         */
        public int get(int index) {
            return this.geno[index];
        }
        
        /** 
         * @return the width of the board
         */
        public int size() {
            return this.geno.length;
        }
        
        /**
        * Prints the genome like this: (<genome>)
        * @param out The PrintStream to print to
        */
        public void printGenome(PrintStream out) {
            out.print("(");
            for (int i=0; i<geno.length; i++)
                out.print("" + geno[i] + "  ");
            out.print(")"); 
        }
        
        public int compareTo(Geno other) {
            int us = this.getConflicts();
            int them = other.getConflicts();
        
            if (us < them) return -1;
            else if (us > them) return 1;
            else return 0;
        }
    }
}