<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>

<s:action name="header" executeResult="true" />
<s:action name="navigation" executeResult="true" />

<div class="d-flex" id="wrapper">
  <table class="table table-striped">
    <thead>
      <tr>
        <th scope="col">Action</th>
        <th scope="col">average generation time</th>
        <th scope="col">total call number</th>
        <th scope="col">total generation time</th>
      </tr>
    </thead>
    <tbody>
      <s:iterator value="performanceInfos">
        <tr>
          <td><s:property value="key" /></td>
          <td><s:property value="averageTime" />ms</td>
          <td><s:property value="count" /></td>
          <td><s:property value="totalTime" />ms</td>
        </tr>
      </s:iterator>
    </tbody>
  </table>
</div>

<s:action name="footer" executeResult="true" />