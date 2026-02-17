package org.apache.soap;

/** Minimal compatibility stub for org.apache.soap.Fault used in tests. */
public class Fault extends Exception
{
   public Fault() { super(); }
   public Fault(String msg) { super(msg); }
   @Override
   public String toString()
   {
      return getMessage();
   }
}
