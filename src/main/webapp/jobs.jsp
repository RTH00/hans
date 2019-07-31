<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>

<s:action name="header" executeResult="true" />
<s:action name="navigation" executeResult="true" />

<div class="d-flex" id="wrapper">
  <!-- Sidebar -->
  <div class="bg-light border-right" id="sidebar-wrapper">
    <div class="list-group">
      <s:iterator value="jobs">
        <s:url var="jobs" action="jobs">
          <s:param name="jobName"><s:property value="name" /></s:param>
        </s:url>
        <a href="${jobs}" class="list-group-item list-group-item-action bg-light"><s:property value="name" /></a>
      </s:iterator>
    </div>
  </div>

  <div class="container-fluid">

  <s:if test="selectedJob != null">
    <!-- selected job -->
    Name: <s:property value="selectedJob.name"/><br>
    Start partition: <s:property value="selectedJob.startPartition"/><br>

    <s:if test="selectedJob.isActivated">
      <form class="btn-group" action="desactivateJob">
        <s:hidden name="jobName" value="%{selectedJob.name}"/>
        <input type="submit" class="btn btn-success active" value="On" />
        <input type="submit" action="" class="btn btn-light text-danger" value="Off" />
      </form>
    </s:if>
    <s:else>
      <form class="btn-group" action="activateJob">
        <s:hidden name="jobName" value="%{selectedJob.name}"/>
        <input type="submit" class="btn nbt-light text-success" value="On" />
        <input type="submit" class="btn btn-danger active" value="Off" />
      </form>
    </s:else>

    <br>
    <br> <!-- TODO add margin rather than <br> -->
    <br>

    <table class="table table-striped">
      <thead>
        <tr>
          <th scope="col">Partition</th>
          <th scope="col">Status</th>
          <th scope="col">Start</th>
          <th scope="col">End</th>
          <th scope="col">Running time</th>
        </tr>
      </thead>
      <tbody>
        <s:iterator value="executionInfos">
          <tr>
            <td scope="row"><s:property value="partition" /></th>
            <s:if test='"SUCCESS".equals(status)'>
              <td class="text-success"><s:property value="status" /></td>
            </s:if>
            <s:elseif test='"FAILURE".equals(status)'>
              <td class="text-danger"><s:property value="status" /></td>
            </s:elseif>
            <s:elseif test='"RUNNING".equals(status)'>
              <td class="text-warning"><s:property value="status" /></td>
            </s:elseif>
       	    <s:else>
              <td><s:property value="status" /></td>
            </s:else>
            <td><s:property value="startTime" /></td>
            <td><s:property value="endTime" /></td>
            <td>
              <s:if test='runningTime != null'>
                <s:property value="runningTime" />s
              </s:if>
            </td>
          </tr>
        </s:iterator>
      </tbody>
    </table>
  </s:if>
  <s:else>
    No job selected.
  </s:else>

  </div>
</div>

<s:action name="footer" executeResult="true" />