<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>

<s:action name="header" executeResult="true" />
<s:action name="navigation" executeResult="true" ignoreContextParams="true">
    <s:param name="page" value='"dashboard"'/>
</s:action>

<div class="container-fluid">

  dashboard here<br>
  insert list of last failing executions<br>

</div>


<s:action name="footer" executeResult="true" />