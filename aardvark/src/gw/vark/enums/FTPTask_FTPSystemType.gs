package gw.vark.enums

enum FTPTask_FTPSystemType{

  NoVal("NoVal"),
  UNIX("UNIX"),
  VMS("VMS"),
  WINDOWS("WINDOWS"),
  OS_2("OS/2"),
  OS_400("OS/400"),
  MVS("MVS"),

  property get Instance() : org.apache.tools.ant.taskdefs.optional.net.FTPTask.FTPSystemType {
    return org.apache.tools.ant.types.EnumeratedAttribute.getInstance(org.apache.tools.ant.taskdefs.optional.net.FTPTask.FTPSystemType, Val) as org.apache.tools.ant.taskdefs.optional.net.FTPTask.FTPSystemType
  }

  var _val : String as Val

  private construct( s : String ) { Val = s }

}
