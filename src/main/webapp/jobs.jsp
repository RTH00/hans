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

    <div class="row mt-2 ml-1">
      <!-- display on/off buttons -->
      <s:if test="selectedJob.isActivated">
        <form class="btn-group" action="desactivateJob">
          <s:hidden name="jobName" value="%{selectedJob.name}"/>
          <input type="submit" class="btn btn-success active" value="Started" />
          <input type="submit" class="btn btn-light text-danger" value="Stop" />
        </form>
      </s:if>
      <s:else>
        <form class="btn-group" action="activateJob">
          <s:hidden name="jobName" value="%{selectedJob.name}"/>
          <input type="submit" class="btn nbt-light text-success" value="Start" />
          <input type="submit" class="btn btn-danger active" value="Stopped" />
        </form>
      </s:else>
    </div>

    <button class="mt-3 btn btn-secondary" type="button" data-toggle="collapse" data-target="#collapsible-job-property-list">
      Show/Hide job properties
    </button>
    <table class="mt-3 table table-bordered collapse" id="collapsible-job-property-list">
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
          <th scope="row">Dependency</th>
          <td>
            <s:if test="dependencies.size() == 0">
              None
            </s:if>
            <s:else>
              <table class="table table-bordered">
                <thead>
                  <tr>
                    <th scope="col">Job name</th>
                    <th scope="col">Shift</th>
                  </tr>
                </thead>
                <tbody>
                  <s:iterator value="dependencies">
                    <tr>
                      <td><s:property value="jobName" /></td>
                      <td><s:property value="shift" /></td>
                    </tr>
                  </s:iterator>
                </tbody>
              </table>
            </s:else>
          </td>
        </tr>
        <tr>
          <th scope="row">Command</th>
          <td>
            <table class="table table-bordered">
              <tbody>
                <s:iterator value="commands">
                  <tr><td><s:property/></tr></td>
                </s:iterator>
              </tbody>
            </table>
          </td>
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
      </tbody>
    </table>

    <button class="mt-3 btn btn-secondary" type="button" data-toggle="collapse" data-target="#collapsible-job-execution-list">
      Show/Hide job executions
    </button>
    <table class="mt-3 table table-striped collapse show" id="collapsible-job-execution-list">
      <thead>
        <tr>
          <th scope="col">Partition</th>
          <th scope="col">Status</th>
          <th scope="col">Start</th>
          <th scope="col">End</th>
          <th scope="col">Running time</th>
          <th scope="col">Rerun</th>
          <th scope="col">Force success</th>
          <th scope="col">Force failure</th>
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
            <td>TODO</td>
            <td>TODO</td>
            <td>TODO</td>
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