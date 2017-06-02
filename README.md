# liferay-clean-journal-history
Groovy script that makes possible to delete or only list old journal articles and its resources from Liferay database. You can set the amount of versions by journal article to save modifying the variable <b>leaveVersionCount</b>. This script has been tested with Liferay 6.0, 6.1 and 6.2.

If you only want to list the journal articles targeted to be deleted you have to set false the variable <b>deleteArticles</b>.

If you only want to process a specific sites, you can set explicitly the groupId's in the variable <b>groupIds</b>, separated by comma.

The steps to execute this script are:

<b>Control_Panel -> Server_Administration -> Script ->  Groovy Script</b>
