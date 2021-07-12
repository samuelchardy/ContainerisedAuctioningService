public class Semaphore
{
    private int count;

    /**
     * Constructor that assigns number of threads allowed access via the semaphore at one time.
     * @param count Number of threads allowed access at any given time.
     */
    public Semaphore(int count)
    {
        this.count = count;
    }


    /**
     * Blocks waiting threads until the current thread/s release the semaphore.
     */
    public synchronized void P()
    {
        while (count == 0){
            try{
                wait();
            }catch(Exception e){}
        }
        count--;
    }


    /**
     * Releases the semaphore to be used by other thread/s.
     */
    public synchronized void V()
    {
        count++;
        notifyAll();
    }

}