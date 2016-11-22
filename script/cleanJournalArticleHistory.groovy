import com.liferay.portal.kernel.log.Log
import com.liferay.portal.kernel.log.LogFactoryUtil
import com.liferay.portal.kernel.dao.orm.*
import com.liferay.portal.kernel.util.ListUtil
import com.liferay.portal.kernel.util.PortalClassLoaderUtil
import com.liferay.portal.kernel.workflow.WorkflowConstants
import com.liferay.portal.model.Group
import com.liferay.portlet.journal.model.JournalArticle
import com.liferay.portal.service.GroupLocalServiceUtil
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil

import java.util.Comparator

class CleanArticleHistory {

    void cleanHistory() throws Exception {
        long countDeleted = 0
        int totalArticlesCount = JournalArticleLocalServiceUtil.getJournalArticlesCount()
        List<Group> toAdd = new ArrayList<Group>()
        toAdd.addAll(GroupLocalServiceUtil.getGroups(10131, "com.liferay.portal.model.Company", 0))
        toAdd.addAll(GroupLocalServiceUtil.getGroups(10131, "com.liferay.portal.model.Group", 0))

        for (Group curGroup : toAdd) {
            _log.info("PROCESANDO GROUP: " + curGroup.getGroupId())
            DynamicQuery differentArticleIdsQuery = DynamicQueryFactoryUtil.forClass(JournalArticle.class, PortalClassLoaderUtil.getClassLoader())
            differentArticleIdsQuery.setProjection(ProjectionFactoryUtil.distinct(PropertyFactoryUtil.forName("articleId")))
            Criterion groupIdCriterion = RestrictionsFactoryUtil.eq("groupId", curGroup.getGroupId())
            differentArticleIdsQuery.add(groupIdCriterion)
            List<Object> differentIds = JournalArticleLocalServiceUtil.dynamicQuery(differentArticleIdsQuery)
            for (Object curId : differentIds) {
                _log.info("PROCESANDO ARTICLEID: " + curId)
                JournalArticle lastVersion = JournalArticleLocalServiceUtil.fetchLatestArticle(curGroup.getGroupId(), (String) curId, WorkflowConstants.STATUS_APPROVED)
                if (lastVersion == null){
                    lastVersion=JournalArticleLocalServiceUtil.fetchLatestArticle(curGroup.getGroupId(), (String) curId, WorkflowConstants.STATUS_ANY)
                    _log.warn("EL CONTENIDO NO TIENE NINGUNA VERSION APROBADA")
                    if(lastVersion==null) continue
                }

                double lastVersionApproved = lastVersion.getVersion()
                _log.info("MAX VERSION: " + lastVersionApproved)
                List<JournalArticle> allVersionArticles = ListUtil.sort(JournalArticleLocalServiceUtil.getArticles(curGroup.getGroupId(), (String) curId), versionComparator)
                int versionsCount = allVersionArticles.size()
                for (JournalArticle article : allVersionArticles) {
                    double version = article.getVersion()
                    if (versionsCount > leaveVersionCount && version < lastVersionApproved) {
                        if (deleteArticles) {
                            JournalArticleLocalServiceUtil.deleteArticle(article)
                        }
                        _log.debug("Borrada version: " + version + " del JournalArticle: " + curId)
                        versionsCount--; countDeleted++
                    }
                }
                _log.info("Borrados: " + countDeleted + " de un total de: " + totalArticlesCount)
            }
        }
        _log.info("BORRADOS TOTALMENTE: " + countDeleted + " DE UN TOTAL DE: " + totalArticlesCount)
    }

    private static Log _log = LogFactoryUtil.getLog(CleanArticleHistory.class)
    private static boolean deleteArticles = true
    private static long companyId = 10131
    private static long leaveVersionCount = 2
    private static Comparator<JournalArticle> versionComparator = new Comparator<JournalArticle>() {
        @Override
        int compare(JournalArticle o1, JournalArticle o2) {
            return Double.compare(o1.getVersion(), o2.getVersion())
        }

        @Override
        boolean equals(Object obj) {
            return this.equals(obj)
        }
    }
}

(new CleanArticleHistory()).cleanHistory()

