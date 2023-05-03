<#include '../fds/layout.ftl'>
<#include '../fds/objects/layouts/leftSubNavLayout.ftl'>

<#macro defaultPage
htmlTitle
caption=""
pageHeading=""
phaseBannerLink="/"
pageSize="TWO_THIRDS_COLUMN"
backLink=false
backLinkUrl=""
>
    <#assign eipScriptContent>
      <link rel="stylesheet" href="<@spring.url'/assets/static/css/ol.css'/>">
    </#assign>

    <#assign serviceName = "File Upload Demo" />
    <#assign customerMnemonic = "FIVIUM" />
    <#assign serviceHomeUrl = "/" />

    <#assign headerContent>
        <@fdsHeader.header
        homePageUrl=serviceHomeUrl
        serviceUrl=serviceHomeUrl
        logoProductText=customerMnemonic
        headerNav=topNavigation
        serviceName=serviceName
        headerLogo="GOV_CREST">
        </@fdsHeader.header>
    </#assign>

    <@fdsDefaultPageTemplate
    htmlTitle=htmlTitle
    serviceName=serviceName
    htmlAppTitle=serviceName
    pageHeading=pageHeading
    headerLogo="GOV_CREST"
    logoProductText=customerMnemonic
    phaseBanner=false
    phaseBannerLink=springUrl(phaseBannerLink)
    serviceUrl=serviceHomeUrl
    homePageUrl=serviceHomeUrl
    twoThirdsColumn=true
    backLink=backLink
    backLinkUrl=backLinkUrl
    caption=caption
    headerContent=headerContent
    >
        <#nested />
    </@fdsDefaultPageTemplate>
</#macro>
