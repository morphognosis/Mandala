// For conditions of distribution and use, see copyright notice in LICENSE.txt

// Utility class.

package mandala;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class Utility
{
   // Load integer.
   public static int loadInt(DataInputStream in) throws IOException
   {
      return(in.readInt());
   }


   // Load float.
   public static float loadFloat(DataInputStream in) throws IOException
   {
      return(in.readFloat());
   }


   // Load double.
   public static double loadDouble(DataInputStream in) throws IOException
   {
      return(in.readDouble());
   }


   // Save string.
   public static String loadString(DataInputStream in) throws IOException
   {
      return(in.readUTF());
   }


   // Load integer.
   public static int loadInt(BufferedReader in) throws IOException
   {
      String line = in.readLine();

      String[] parts = line.split("#");
      if ((parts != null) && (parts.length > 0))
      {
         try
         {
            return(Integer.parseInt(parts[0]));
         }
         catch (NumberFormatException e)
         {
            throw new IOException();
         }
      }
      throw new IOException();
   }


   // Load float.
   public static float loadFloat(BufferedReader in) throws IOException
   {
      String line = in.readLine();

      String[] parts = line.split("#");
      if ((parts != null) && (parts.length > 0))
      {
         try
         {
            return(Float.parseFloat(parts[0]));
         }
         catch (NumberFormatException e)
         {
            throw new IOException();
         }
      }
      throw new IOException();
   }


   // Save integer.
   public static void saveInt(DataOutputStream out, int value) throws IOException
   {
      out.writeInt(value);
   }


   // Save float.
   public static void saveFloat(DataOutputStream out, float value) throws IOException
   {
      out.writeFloat(value);
   }


   // Save double.
   public static void saveDouble(DataOutputStream out, double value) throws IOException
   {
      out.writeDouble(value);
   }


   // Save string.
   public static void saveString(DataOutputStream out, String value) throws IOException
   {
      out.writeUTF(value);
   }


   // Save integer.
   public static void saveInt(PrintWriter out, int value, String description) throws IOException
   {
      out.println(value + "#" + description);
   }


   public static void saveInt(PrintWriter out, int value) throws IOException
   {
      out.println(value + "#");
   }


   // Save float.
   public static void saveFloat(PrintWriter out, float value, String description) throws IOException
   {
      out.println(value + "#" + description);
   }


   public static void saveFloat(PrintWriter out, float value) throws IOException
   {
      out.println(value + "#");
   }


   // Prevent instantiation.
   private Utility() {}
}
