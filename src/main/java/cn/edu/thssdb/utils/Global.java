package cn.edu.thssdb.utils;

import java.util.ArrayList;
import java.util.List;

public class Global {
  public static int fanout = 129;

  public static int SUCCESS_CODE = 0;
  public static int FAILURE_CODE = -1;

  public static String DEFAULT_SERVER_HOST = "127.0.0.1";
  public static int DEFAULT_SERVER_PORT = 6667;

  public static String CLI_PREFIX = "ThssDB>";
  public static final String SHOW_TIME = "show time;";
  public static final String QUIT = "quit;";

  public static final String S_URL_INTERNAL = "jdbc:default:connection";

  //用于转换Object为数组类型
  //https://www.cnblogs.com/xingmangdieyi110/p/11676553.html
  public static <T> List<T> castList(Object obj, Class<T> clazz) throws ClassCastException
  {
    List<T> result = new ArrayList<T>();
    if(obj instanceof List<?>)
    {
      for (Object o : (List<?>) obj)
      {
        result.add(clazz.cast(o));
      }
      return result;
    }
    return null;
  }
}
