package online4m.apigateway.si

class Utils {

  private static final Set simpleTypes = new HashSet([
    Boolean.class,
    Character.class,
    Byte.class,
    Short.class,
    Integer.class,
    Long.class,
    Float.class,
    Double.class,
    Void.class,
    String.class
  ])

  static boolean isSimpleType(Class clazz) {
    simpleTypes.contains(clazz)
  }
}
