<#include '../layout/layout.ftlh'>

<#assign pageTitle = "Consent details"/>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
>
  <ul class="govuk-list">
      <#list consentList![] as consent>
        <li>
            <@fdsAction.link
            linkUrl="${consent.id}"
            linkText="${consent.id}"
            />
        </li>
      </#list>
  </ul>
    <@fdsForm.htmlForm>
        <@fdsAction.button buttonText="Create new consent"/>
    </@fdsForm.htmlForm>
</@defaultPage>
