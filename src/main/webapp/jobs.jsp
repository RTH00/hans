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

    <table class="table table-bordered">
      <thead>
        <tr>
          <th scope="col">Property</th>
          <th scope="col">Value</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <th scope="row">Name</th>
          <td><s:property value="selectedJob.name"/></td>
        </tr>
        <tr>
          <th scope="row">Start partition</th>
          <td><s:property value="selectedJob.startPartition"/></td>
        </tr>
        <tr>
          <th scope="row">End partition</th>
          <td><s:property value="selectedJob.endPartition"/></td>
        </tr>
        <tr>
          <th scope="row">Increment</th>
          <td><s:property value="selectedJob.increment"/></td>
        </tr>
        <tr>
          <th scope="row">Max parallelism</th>
          <td><s:property value="selectedJob.maxParallelism"/></td>
        </tr>
        <tr>
          <th scope="row">Failure behavior</th>
          <td><s:property value="selectedJob.failureBehavior"/></td>
        </tr>
        <tr>
          <th scope="row">Retry delay</th>
          <td><s:property value="selectedJob.retryDelay"/></td>
        </tr>
        <tr>
          <th scope="row">Retention</th>
          <td><s:property value="selectedJob.retention"/></td>
        </tr>
        <tr>
          <th scope="row">Stdout path</th>
          <td><s:property value="selectedJob.stdoutPath"/></td>
        </tr>
        <tr>
          <th scope="row">Stderr path</th>
          <td><s:property value="selectedJob.stderrPath"/></td>
        </tr>
        <tr>
          <th scope="row">Status</th>
          <td>
            <!-- display on/off buttons -->
            <s:if test="selectedJob.isActivated">
              <form class="btn-group" action="desactivateJob">
                <s:hidden name="jobName" value="%{selectedJob.name}"/>
                <input type="submit" class="btn btn-success active" value="On" />
                <input type="submit" class="btn btn-light text-danger" value="Off" />
              </form>
            </s:if>
            <s:else>
              <form class="btn-group" action="activateJob">
                <s:hidden name="jobName" value="%{selectedJob.name}"/>
                <input type="submit" class="btn nbt-light text-success" value="On" />
                <input type="submit" class="btn btn-danger active" value="Off" />
              </form>
            </s:else>
          </td>
        </tr>
      </tbody>
    </table>

    <h4>Executions:</h4>

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
        <!-- list of executions -->
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