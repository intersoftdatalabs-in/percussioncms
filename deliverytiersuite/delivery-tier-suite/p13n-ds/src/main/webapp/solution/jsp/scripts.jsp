<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%-- Issue #5668: the vendored lib-js tree (jQuery 1.3.2, jquery-treeview,
     jquery-form, jquery-splitter, jquery-dialog/simplemodal, json2.js,
     log4javascript) was removed. Those libraries are dead upstream and/or
     carry known XSS hardening gaps (pre-3.5 jQuery), and this legacy webapp
     is not part of the Maven build. Do NOT reintroduce vendored copies:
     supply a modern (3.5+) npm-tracked jQuery plus supported plugin builds
     if this editor page is ever rewired to a live deployment. --%>
<script type="text/javascript"
	src="${scripts}/p13n/perc_p13n_profile.js"></script>

<script type="text/javascript">
	var p13nInsideRhythmyx = ${insideRx};
</script>