// Graph class representing a directed
// graph using adjacency list
// See: https://www.geeksforgeeks.org/java/java-program-for-detect-cycle-in-a-directed-graph/

package mandala;

import java.util.ArrayList;
import java.util.List;

class Graph {
   private final int vertices;
   private final     List < List < Integer >> adjList;

   public Graph(int vertices)
   {
      this.vertices = vertices;
      adjList       = new ArrayList<>(vertices);
      for (int i = 0; i < vertices; i++)
      {
         adjList.add(new ArrayList<>());
      }
   }


   public void addEdge(int src, int dest)
   {
      adjList.get(src).add(dest);
   }


   public boolean removeEdge(int src, int dest)
   {
      List<Integer> edges = adjList.get(src);
      for (int i = 0, j = edges.size(); i < j; i++)
      {
         if (edges.get(i) == dest)
         {
            edges.remove(i);
            return(true);
         }
      }
      return(false);
   }


   public List<Integer> getNeighbors(int vertex)
   {
      return(adjList.get(vertex));
   }


   public int getVertices()
   {
      return(vertices);
   }


   private boolean dfs(int vertex, boolean[] visited, boolean[] recStack)
   {
      if (recStack[vertex])
      {
         return(true);
      }
      if (visited[vertex])
      {
         return(false);
      }

      visited[vertex]  = true;
      recStack[vertex] = true;

      for (int neighbor : getNeighbors(vertex))
      {
         if (dfs(neighbor, visited, recStack))
         {
            return(true);
         }
      }

      recStack[vertex] = false;
      return(false);
   }


   public boolean hasCycle()
   {
      int vertices = getVertices();

      boolean[] visited  = new boolean[vertices];
      boolean[] recStack = new boolean[vertices];

      for (int i = 0; i < vertices; i++)
      {
         if (!visited[i] && dfs(i, visited, recStack))
         {
            return(true);
         }
      }
      return(false);
   }


   public void audit()  // flibber
   {
      for (int i = 0; i < vertices; i++)
      {
         List<Integer> n = getNeighbors(i);
         System.out.print("edges of " + i + ": ");
         for (Integer x : n)
         {
            System.out.print(x + " ");
         }
         System.out.println();
      }
   }
}
