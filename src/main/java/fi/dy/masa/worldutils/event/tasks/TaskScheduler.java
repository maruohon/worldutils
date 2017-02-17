package fi.dy.masa.worldutils.event.tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class TaskScheduler
{
    private static TaskScheduler instance;
    private List<ITask> tasks = new ArrayList<ITask>();
    private List<Timer> timers = new ArrayList<Timer>();
    private List<Pair<ITask, Integer>> tasksToAdd = new ArrayList<Pair<ITask, Integer>>();

    private TaskScheduler()
    {
    }

    public static TaskScheduler getInstance()
    {
        if (instance == null)
        {
            instance = new TaskScheduler();
        }

        return instance;
    }

    public void scheduleTask(ITask task, int interval)
    {
        this.tasksToAdd.add(Pair.of(task, interval));
    }

    public void runTasks()
    {
        Iterator<ITask> taskIter = this.tasks.iterator();
        Iterator<Timer> timerIter = this.timers.iterator();

        while (taskIter.hasNext())
        {
            boolean finished = false;
            ITask task = taskIter.next();
            Timer timer = timerIter.next();

            if (timer.tick())
            {
                if (task.canExecute())
                {
                    finished = task.execute();
                }
                else
                {
                    finished = true;
                }
            }

            if (finished)
            {
                task.stop();
                taskIter.remove();
                timerIter.remove();
            }
        }

        this.addNewTasks();
    }

    private void addNewTasks()
    {
        for (Pair<ITask, Integer> pair : this.tasksToAdd)
        {
            ITask task = pair.getLeft();
            task.init();

            this.tasks.add(task);
            this.timers.add(new Timer(pair.getRight()));
        }

        this.tasksToAdd.clear();
    }

    public boolean hasTasks()
    {
        return this.tasks.isEmpty() == false || this.tasksToAdd.isEmpty() == false;
    }

    public boolean hasTask(Class <? extends ITask> clazz)
    {
        for (ITask task : this.tasks)
        {
            if (clazz.equals(task.getClass()))
            {
                return true;
            }
        }

        return false;
    }

    public boolean removeTask(Class <? extends ITask> clazz)
    {
        boolean removed = false;
        Iterator<ITask> taskIter = this.tasks.iterator();
        Iterator<Timer> timerIter = this.timers.iterator();

        while (taskIter.hasNext())
        {
            ITask task = taskIter.next();
            timerIter.next();

            if (clazz.equals(task.getClass()))
            {
                task.stop();
                taskIter.remove();
                timerIter.remove();
                removed = true;
            }
        }

        return removed;
    }

    public void clearTasks()
    {
    }

    private static class Timer
    {
        public int interval;
        public int counter;

        public Timer(int interval)
        {
            this.interval = interval;
            this.counter = interval;
        }

        public boolean tick()
        {
            if (--this.counter <= 0)
            {
                this.reset();
                return true;
            }

            return false;
        }

        public void reset()
        {
            this.counter = this.interval;
        }
    }
}
