<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags" %>

<s:action name="header" executeResult="true" />

<div class="row">
  <br><br><br>
</div>

<div class="row justify-content-center">
  <div class="col-md-4 mt-5">
    <div class="text-center mb-4">
      <a href="/index">
        <img class="mb-4" src="/image/hans-logo.png" height="72">
      </a>
      <h1 class="h3 mb-3 font-weight-normal">Sign in</h1>
      <p>Enter your username and password to sign in</p>
    </div>

    <s:form id="registerForm" action="login" theme="bootstrap">
      <s:textfield label="Username" name="username"/>
      <s:password label="Password" name="password"/>
      <s:actionerror theme="bootstrap"/>
      <div class="form-group">
        <button class="btn btn-lg btn-primary btn-block" type="submit">Sign in</button>
      </div>
    </s:form>
  </div>
</div>

<s:action name="footer" executeResult="true" />