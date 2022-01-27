package generator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

class Graph
{
    private int v;  
    private LinkedList<Integer> adj[]; 
  
   
    Graph(int v)
    {
        this.v = v;
        adj = new LinkedList[v];
        for (int i=0; i<v; ++i)
            adj[i] = new LinkedList();
    }
  
    
    void addEdge(int v,int w) { adj[v].add(w); }
  
   
    void topologicalSortUtil(int v, boolean visited[], Stack<Integer> stack)
    {
       
        visited[v] = true;
        Integer i;  
       
        Iterator<Integer> it = adj[v].iterator();
        while (it.hasNext())
        {
            i = it.next();
            if (!visited[i])
                topologicalSortUtil(i, visited, stack);
        }
          
        stack.push(v);
    }
  
    Stack<Integer> topologicalSort()
    {
        Stack<Integer> stack = new Stack<Integer>();
          
        boolean visited[] = new boolean[v];
        for (int i = 0; i < v; i++)
            visited[i] = false;
  
        for (int i = 0; i < v; i++)
            if (visited[i] == false)
                topologicalSortUtil(i, visited, stack);
  
        return stack;
    }
    
}
    