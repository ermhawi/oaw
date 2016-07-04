package es.inteco.rastreador2.utils;

import ca.utoronto.atrc.tile.accessibilitychecker.Evaluation;
import ca.utoronto.atrc.tile.accessibilitychecker.Evaluator;
import ca.utoronto.atrc.tile.accessibilitychecker.EvaluatorUtility;
import com.opensymphony.oscache.base.NeedsRefreshException;
import es.inteco.common.Constants;
import es.inteco.common.logging.Logger;
import es.inteco.common.properties.PropertiesManager;
import es.inteco.intav.datos.AnalisisDatos;
import es.inteco.intav.form.*;
import es.inteco.intav.utils.CacheUtils;
import es.inteco.intav.utils.EvaluatorUtils;
import es.inteco.plugin.dao.DataBaseManager;
import es.inteco.rastreador2.actionform.observatorio.ModalityComparisonForm;
import es.inteco.rastreador2.actionform.observatorio.ObservatorioForm;
import es.inteco.rastreador2.actionform.rastreo.FulfilledCrawlingForm;
import es.inteco.rastreador2.actionform.semillas.CategoriaForm;
import es.inteco.rastreador2.dao.observatorio.ObservatorioDAO;
import es.inteco.rastreador2.dao.rastreo.RastreoDAO;
import es.inteco.view.forms.CategoryViewListForm;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.util.MessageResources;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static es.inteco.common.Constants.CRAWLER_PROPERTIES;

public final class ResultadosAnonimosObservatorioUNE2012Utils {

    private ResultadosAnonimosObservatorioUNE2012Utils() {
    }

    //GENERATE GRAPHIC METHODS
    private static int x = 0;
    private static int y = 0;

    static {
        PropertiesManager pmgr = new PropertiesManager();
        x = Integer.parseInt(pmgr.getValue(CRAWLER_PROPERTIES, "chart.observatory.graphic.x"));
        y = Integer.parseInt(pmgr.getValue(CRAWLER_PROPERTIES, "chart.observatory.graphic.y"));
    }

    public static void generateGraphics(final HttpServletRequest request, final String filePath, final String type, final boolean regenerate) throws Exception {
        Connection c = null;
        try {
            final PropertiesManager pmgr = new PropertiesManager();
            String color = pmgr.getValue(CRAWLER_PROPERTIES, "chart.evolution.inteco.red.colors");
            if (type != null && type.equals(Constants.MINISTERIO_P)) {
                color = pmgr.getValue(CRAWLER_PROPERTIES, "chart.evolution.mp.green.color");
            }
            c = DataBaseManager.getConnection();
            //recuperamos las categorias del observatorio
            final List<CategoriaForm> categories = ObservatorioDAO.getExecutionObservatoryCategories(c, Long.valueOf(request.getParameter(Constants.ID)));
            generateGlobalGraphics(request, filePath, categories, color, regenerate);
            //iteramos sobre ellas y genermos las gráficas
            for (CategoriaForm categoryForm : categories) {
                generateCategoryGraphics(request, categoryForm, filePath, color, regenerate);
            }
            generateEvolutionGraphics(request, filePath, color, regenerate);
        } catch (Exception e) {
            Logger.putLog("No se han generado las gráficas correctamente.", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
            throw e;
        } finally {
            DataBaseManager.closeConnection(c);
        }
    }

    public static int generateGlobalGraphics(final HttpServletRequest request, final String filePath, final List<CategoriaForm> categories, final String color, final boolean regenerate) throws Exception {
        final String executionId = request.getParameter(Constants.ID);
        final List<ObservatoryEvaluationForm> pageExecutionList = getGlobalResultData(executionId, Constants.COMPLEXITY_SEGMENT_NONE, null);

        if (pageExecutionList != null && !pageExecutionList.isEmpty()) {
            final MessageResources messageResources = CrawlerUtils.getResources(request);
            final String noDataMess = messageResources.getMessage(CrawlerUtils.getLocale(request), "grafica.sin.datos");

            String title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.accessibility.level.allocation.title");
            String file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.accessibility.level.allocation.name") + ".jpg";
            getGlobalAccessibilityLevelAllocationSegmentGraphic(request, pageExecutionList, title, file, noDataMess, regenerate);

            title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.global.puntuation.allocation.segment.strached.title");
            file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.global.puntuation.allocation.segment.strached.name") + ".jpg";
            getGlobalMarkBySegmentGraphic(request, pageExecutionList, title, file, noDataMess, categories);

            title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.modality.by.verification.level.1.title");
            file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.modality.by.verification.level.1.name") + ".jpg";
            getModalityByVerificationLevelGraphic(request, pageExecutionList, title, file, noDataMess, Constants.OBS_PRIORITY_1, regenerate);

            title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.modality.by.verification.level.2.title");
            file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.modality.by.verification.level.2.name") + ".jpg";
            getModalityByVerificationLevelGraphic(request, pageExecutionList, title, file, noDataMess, Constants.OBS_PRIORITY_2, regenerate);

            file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.global.puntuation.allocation.segments.mark.name") + ".jpg";
            getGlobalMarkBySegmentsGroupGraphic(request, file, noDataMess, pageExecutionList, categories, regenerate);

            file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.aspect.mid.name") + ".jpg";
            title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.aspect.mid.title");
            getAspectMidsGraphic(request, file, noDataMess, pageExecutionList, color, title, regenerate);

            title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.verification.mid.comparation.level.1.title");
            file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.verification.mid.comparation.level.1.name") + ".jpg";
            getMidsComparationByVerificationLevelGraphic(request, Constants.OBS_PRIORITY_1, title, file, noDataMess, pageExecutionList, color, regenerate);

            title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.verification.mid.comparation.level.2.title");
            file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.verification.mid.comparation.level.2.name") + ".jpg";
            getMidsComparationByVerificationLevelGraphic(request, Constants.OBS_PRIORITY_2, title, file, noDataMess, pageExecutionList, color, regenerate);

            return Constants.OBSERVATORY_HAVE_RESULTS;
        } else {
            return Constants.OBSERVATORY_NOT_HAVE_RESULTS;
        }
    }

    public static int generateCategoryGraphics(HttpServletRequest request, CategoriaForm category, String filePath, String color, boolean regenerate) throws Exception {

        Connection conn = null;
        try {
            conn = DataBaseManager.getConnection();

            final String idExecution = request.getParameter(Constants.ID);
            final MessageResources messageResources = CrawlerUtils.getResources(request);
            final String noDataMess = messageResources.getMessage(CrawlerUtils.getLocale(request), "grafica.sin.datos");
            final List<ObservatoryEvaluationForm> pageExecutionList = getGlobalResultData(idExecution, Long.parseLong(category.getId()), null);

            if (pageExecutionList != null && !pageExecutionList.isEmpty()) {
                String title = messageResources.getMessage("observatory.graphic.accessibility.level.allocation.segment.title", category.getName());
                String file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.accessibility.level.allocation.segment.name", category.getOrden()) + ".jpg";
                getGlobalAccessibilityLevelAllocationSegmentGraphic(request, pageExecutionList, title, file, noDataMess, regenerate);

                title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.mark.allocation.segment.title", category.getName());
                file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.mark.allocation.segment.name", category.getOrden()) + ".jpg";
                List<ObservatorySiteEvaluationForm> result = getSitesListByLevel(pageExecutionList);
                //if (observatoryType == Constants.OBSERVATORY_TYPE_CCAA){
                //getMarkAllocationLevelSegmentGraphic(request, title, file, noDataMess, result, true, regenerate);
                //}else{
                getMarkAllocationLevelSegmentGraphic(request, title, file, noDataMess, result, false, regenerate);
                //}

                file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.aspect.mid.name") + category.getOrden() + ".jpg";
                title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.segment.aspect.mid.title", category.getName());
                getAspectMidsGraphic(request, file, noDataMess, pageExecutionList, color, title, regenerate);

                title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.verification.mid.comparation.level.1.cat.title", category.getName());
                file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.verification.mid.comparation.level.1.name") + category.getOrden() + ".jpg";
                getMidsComparationByVerificationLevelGraphic(request, Constants.OBS_PRIORITY_1, title, file, noDataMess, pageExecutionList, color, regenerate);

                title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.verification.mid.comparation.level.2.cat.title", category.getName());
                file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.verification.mid.comparation.level.2.name") + category.getOrden() + ".jpg";
                getMidsComparationByVerificationLevelGraphic(request, Constants.OBS_PRIORITY_2, title, file, noDataMess, pageExecutionList, color, regenerate);

                title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.modality.by.verification.level.1.category.title", category.getName());
                file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.modality.by.verification.level.1.name") + category.getOrden() + ".jpg";
                getModalityByVerificationLevelGraphic(request, pageExecutionList, title, file, noDataMess, Constants.OBS_PRIORITY_1, regenerate);

                title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.modality.by.verification.level.2.category.title", category.getName());
                file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.modality.by.verification.level.2.name") + category.getOrden() + ".jpg";
                getModalityByVerificationLevelGraphic(request, pageExecutionList, title, file, noDataMess, Constants.OBS_PRIORITY_2, regenerate);

                return Constants.OBSERVATORY_HAVE_RESULTS;
            } else {
                return Constants.OBSERVATORY_NOT_HAVE_RESULTS;
            }
        } catch (Exception e) {
            Logger.putLog("Exception: ", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
            return Constants.OBSERVATORY_NOT_HAVE_RESULTS;
        } finally {
            DataBaseManager.closeConnection(conn);
        }
    }

    public static int generateEvolutionGraphics(HttpServletRequest request, String filePath, String color, boolean regenerate) throws Exception {
        final String observatoryId = request.getParameter(Constants.ID_OBSERVATORIO);
        final String executionId = request.getParameter(Constants.ID);
        final Map<Date, List<ObservatoryEvaluationForm>> pageObservatoryMap = resultEvolutionData(Long.valueOf(observatoryId), Long.valueOf(executionId));

        if (pageObservatoryMap != null && !pageObservatoryMap.isEmpty()) {
            if (pageObservatoryMap.size() != 1) {
                final MessageResources messageResources = CrawlerUtils.getResources(request);
                final String noDataMess = messageResources.getMessage(CrawlerUtils.getLocale(request), "grafica.sin.datos");

                String title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.accessibility.evolution.approval.A.title");
                String file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.accesibility.evolution.approval.A.name") + ".jpg";
                getApprovalLevelEvolutionGraphic(request, Constants.OBS_A, title, file, noDataMess, pageObservatoryMap, color, regenerate);

                title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.accessibility.evolution.approval.AA.title");
                file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.accesibility.evolution.approval.AA.name") + ".jpg";
                getApprovalLevelEvolutionGraphic(request, Constants.OBS_AA, title, file, noDataMess, pageObservatoryMap, color, regenerate);

                title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.accessibility.evolution.approval.NV.title");
                file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.accesibility.evolution.approval.NV.name") + ".jpg";
                getApprovalLevelEvolutionGraphic(request, Constants.OBS_NV, title, file, noDataMess, pageObservatoryMap, color, regenerate);

                file = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.evolution.mid.puntuation.name") + ".jpg";
                getMidMarkEvolutionGraphic(request, noDataMess, file, pageObservatoryMap, color, regenerate);

                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_111_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_112_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_113_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_114_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_115_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_116_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_117_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_121_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_122_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_123_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_211_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_212_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_213_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_214_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_215_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_216_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_217_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_221_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_222_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);
                getMidMarkVerificationEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_EVOLUTION_223_VERIFICATION, noDataMess, filePath, pageObservatoryMap, color, regenerate);

                Map<Date, Map<String, BigDecimal>> resultsByAspect = new HashMap<Date, Map<String, BigDecimal>>();
                for (Map.Entry<Date, List<ObservatoryEvaluationForm>> entry : pageObservatoryMap.entrySet()) {
                    resultsByAspect.put(entry.getKey(), aspectMidsPuntuationGraphicData(messageResources, entry.getValue()));
                }
                getMidMarkAspectEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_ASPECT_ALTERNATIVE_ID, noDataMess, filePath, resultsByAspect, color, regenerate);
                getMidMarkAspectEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_ASPECT_GENERAL_ID, noDataMess, filePath, resultsByAspect, color, regenerate);
                getMidMarkAspectEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_ASPECT_NAVIGATION_ID, noDataMess, filePath, resultsByAspect, color, regenerate);
                getMidMarkAspectEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_ASPECT_PRESENTATION_ID, noDataMess, filePath, resultsByAspect, color, regenerate);
                getMidMarkAspectEvolutionGraphic(request, Constants.OBSERVATORY_GRAPHIC_ASPECT_STRUCTURE_ID, noDataMess, filePath, resultsByAspect, color, regenerate);

                return Constants.OBSERVATORY_HAVE_RESULTS;
            } else {
                return Constants.OBSERVATORY_HAVE_ONE_RESULT;
            }
        } else {
            return Constants.OBSERVATORY_NOT_HAVE_RESULTS;
        }
    }

    public static List<GraphicData> infoGlobalAccessibilityLevel(final MessageResources messageResources,
                                                                 final Map<String, Integer> result) throws Exception {
        final List<GraphicData> labelValueList = new ArrayList<GraphicData>();
        final int totalPort = result.get(Constants.OBS_A) + result.get(Constants.OBS_AA) + result.get(Constants.OBS_NV);

        labelValueList.add(infoGlobalAccessibilityLevelGraphicData(messageResources.getMessage("resultados.anonimos.porc.portales.aa"), result.get(Constants.OBS_AA), totalPort));
        labelValueList.add(infoGlobalAccessibilityLevelGraphicData(messageResources.getMessage("resultados.anonimos.porc.portales.a"), result.get(Constants.OBS_A), totalPort));
        labelValueList.add(infoGlobalAccessibilityLevelGraphicData(messageResources.getMessage("resultados.anonimos.porc.portales.parcial"), result.get(Constants.OBS_NV), totalPort));

        return labelValueList;
    }

    private static GraphicData infoGlobalAccessibilityLevelGraphicData(final String title, final Integer result, final int totalPort) {
        final GraphicData labelValue = new GraphicData();
        labelValue.setAdecuationLevel(title);
        if (totalPort > 0) {
            labelValue.setPercentageP(String.valueOf((new BigDecimal(result).multiply(BigDecimal.valueOf(100)).divide(new BigDecimal(totalPort), 2, BigDecimal.ROUND_HALF_UP))).replace(".00", "") + "%");
        }
        labelValue.setNumberP(String.valueOf(new BigDecimal(result)));
        return labelValue;
    }

    public static List<LabelValueBean> infoComparisonBySegment(final MessageResources messageResources,
                                                               final Map<String, BigDecimal> category) throws Exception {
        final List<LabelValueBean> labelValueList = new ArrayList<LabelValueBean>();

        LabelValueBean labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("resultados.anonimos.punt.portales.aa"));
        if (category.get(Constants.OBS_AA) != null && category.get(Constants.OBS_AA).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(category.get(Constants.OBS_AA)).replace(".00", ""));
        } else if (category.get(Constants.OBS_AA) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("resultados.anonimos.punt.portales.a"));
        if (category.get(Constants.OBS_A) != null && category.get(Constants.OBS_A).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(category.get(Constants.OBS_A)).replace(".00", ""));
        } else if (category.get(Constants.OBS_A) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("resultados.anonimos.punt.portales.parcial"));
        if (category.get(Constants.OBS_NV) != null && category.get(Constants.OBS_NV).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(category.get(Constants.OBS_NV)).replace(".00", ""));
        } else if (category.get(Constants.OBS_NV) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        return labelValueList;
    }

    public static List<LabelValueBean> infoComparisonBySegmentPuntuation(final MessageResources messageResources,
                                                                         final Map<String, BigDecimal> result) throws Exception {
        final List<LabelValueBean> labelValueList = new ArrayList<LabelValueBean>();
        labelValueList.add(new LabelValueBean(messageResources.getMessage("resultados.anonimos.punt.portales.aa"),String.valueOf(result.get(Constants.OBS_AA)).replace(".00", "")));
        labelValueList.add(new LabelValueBean(messageResources.getMessage("resultados.anonimos.punt.portales.a"),String.valueOf(result.get(Constants.OBS_A)).replace(".00", "")));
        labelValueList.add(new LabelValueBean(messageResources.getMessage("resultados.anonimos.punt.portales.parcial"),String.valueOf(result.get(Constants.OBS_NV)).replace(".00", "")));

        return labelValueList;
    }

    public static List<LabelValueBean> infoAspectMidsComparison(final MessageResources messageResources, final Map<String, BigDecimal> result) throws Exception {
        final List<LabelValueBean> labelValueList = new ArrayList<LabelValueBean>();

        LabelValueBean labelValue = new LabelValueBean();

        labelValue.setLabel(messageResources.getMessage("resultados.anonimos.general"));
        if (result.get(messageResources.getMessage("observatory.aspect.general")) != null &&
                result.get(messageResources.getMessage("observatory.aspect.general")).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(messageResources.getMessage("observatory.aspect.general"))).replace(".00", ""));
        } else if (result.get(messageResources.getMessage("observatory.aspect.general")) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("resultados.anonimos.presentacion"));
        if (result.get(messageResources.getMessage("observatory.aspect.presentation")) != null &&
                result.get(messageResources.getMessage("observatory.aspect.presentation")).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(messageResources.getMessage("observatory.aspect.presentation"))).replace(".00", ""));
        } else if (result.get(messageResources.getMessage("observatory.aspect.presentation")) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);


        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("resultados.anonimos.estructura"));
        if (result.get(messageResources.getMessage("observatory.aspect.structure")) != null &&
                result.get(messageResources.getMessage("observatory.aspect.structure")).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(messageResources.getMessage("observatory.aspect.structure"))).replace(".00", ""));
        } else if (result.get(messageResources.getMessage("observatory.aspect.structure")) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("resultados.anonimos.navegacion"));
        if (result.get(messageResources.getMessage("observatory.aspect.navigation")) != null &&
                result.get(messageResources.getMessage("observatory.aspect.navigation")).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(messageResources.getMessage("observatory.aspect.navigation"))).replace(".00", ""));
        } else if (result.get(messageResources.getMessage("observatory.aspect.navigation")) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("resultados.anonimos.alternativa"));
        if (result.get(messageResources.getMessage("observatory.aspect.alternatives")) != null &&
                result.get(messageResources.getMessage("observatory.aspect.alternatives")).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(messageResources.getMessage("observatory.aspect.alternatives"))).replace(".00", ""));
        } else if (result.get(messageResources.getMessage("observatory.aspect.alternatives")) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        return labelValueList;
    }

    public static List<LabelValueBean> infoMarkAllocationLevelSegment(final List<ObservatorySiteEvaluationForm> siteList) {
        final List<LabelValueBean> labelValueList = new ArrayList<LabelValueBean>();
        LabelValueBean labelValue;
        for (ObservatorySiteEvaluationForm siteForm : siteList) {
            labelValue = new LabelValueBean();
            if (siteForm.getAcronym() != null && !StringUtils.isEmpty(siteForm.getAcronym())) {
                labelValue.setLabel(siteForm.getAcronym() + " (" + siteForm.getName() + ")");
            } else {
                labelValue.setLabel(siteForm.getName());
            }
            labelValue.setValue(String.valueOf(siteForm.getScore()));
            labelValueList.add(labelValue);
        }

        return labelValueList;
    }

    public static List<LabelValueBean> infoLevelIVerificationMidsComparison(final MessageResources messageResources,
                                                                            final Map<String, BigDecimal> result) throws Exception {
        final List<LabelValueBean> labelValueList = new ArrayList<LabelValueBean>();

        LabelValueBean labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.1.1.1"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_111_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_111_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_111_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_111_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.1.1.2"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_112_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_112_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_112_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_112_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.1.1.3"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_113_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_113_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_113_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_113_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.1.1.4"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_114_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_114_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_114_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_114_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.1.1.5"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_115_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_115_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_115_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_115_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.1.1.6"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_116_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_116_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_116_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_116_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.1.1.7"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_117_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_117_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_117_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_117_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.1.2.1"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_121_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_121_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_121_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_121_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.1.2.2"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_122_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_122_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_122_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_122_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.1.2.3"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_123_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_123_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_123_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_123_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        return labelValueList;
    }

    public static List<LabelValueBean> infoLevelIIVerificationMidsComparison(final MessageResources messageResources,
                                                                             final Map<String, BigDecimal> result) throws Exception {
        final List<LabelValueBean> labelValueList = new ArrayList<LabelValueBean>();

        LabelValueBean labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.2.1.1"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_211_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_211_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_211_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_211_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.2.1.2"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_212_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_212_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_212_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_212_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.2.1.3"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_213_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_213_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_213_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_213_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.2.1.4"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_214_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_214_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_214_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_214_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.2.1.5"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_215_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_215_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_215_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_215_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.2.1.6"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_216_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_216_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_216_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_216_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.2.1.7"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_217_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_217_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_217_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_217_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.2.2.1"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_221_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_221_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_221_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_221_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.2.2.2"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_222_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_222_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_222_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_222_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        labelValue = new LabelValueBean();
        labelValue.setLabel(messageResources.getMessage("minhap.observatory.2_0.subgroup.2.2.3"));
        if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_223_VERIFICATION) != null &&
                result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_223_VERIFICATION).compareTo(new BigDecimal(-1)) != 0) {
            labelValue.setValue(String.valueOf(result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_223_VERIFICATION)).replace(".00", ""));
        } else if (result.get(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_223_VERIFICATION) == null) {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
        } else {
            labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
        }
        labelValueList.add(labelValue);

        return labelValueList;
    }

    public static List<LabelValueBean> infoLevelEvolutionGraphic(final Map<String, BigDecimal> resultData) {
        return infoEvolutionGraphic(resultData, "%");
    }

    public static List<LabelValueBean> infoMidMarkEvolutionGraphic(final Map<String, BigDecimal> resultData) {
        return infoEvolutionGraphic(resultData, "");
    }

    private static List<LabelValueBean> infoEvolutionGraphic(final Map<String, BigDecimal> resultData, final String suffix) {
        final List<LabelValueBean> labelValueList = new ArrayList<LabelValueBean>();

        for (Map.Entry<String, BigDecimal> entry : resultData.entrySet()) {
            final LabelValueBean labelValue = new LabelValueBean();
            labelValue.setLabel(entry.getKey());
            labelValue.setValue(String.valueOf(entry.getValue()).replace(".00", "") + suffix);
            labelValueList.add(labelValue);
        }

        return labelValueList;
    }

    //RESULTS METHODS

    public static void getMidsComparationByVerificationLevelGraphic(final HttpServletRequest request,
                                                                    final String level, final String title, final String filePath, final String noDataMess, final List<ObservatoryEvaluationForm> pageExecutionList, final String color, final boolean regenerate) throws Exception {
        final File file = new File(filePath);
        final Map<String, BigDecimal> result = getVerificationResultsByPoint(pageExecutionList, level);
        final MessageResources messageResources = CrawlerUtils.getResources(request);
        //Incluimos los resultados en la request
        if (level.equals(Constants.OBS_PRIORITY_1)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_GLOBAL_DATA_LIST_CMVI, infoLevelIVerificationMidsComparison(messageResources, result));
        } else if (level.equals(Constants.OBS_PRIORITY_2)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_GLOBAL_DATA_LIST_CMVII, infoLevelIIVerificationMidsComparison(messageResources, result));
        }

        //Si no existe la gráfica, la creamos
        if (!file.exists() || regenerate) {
            final String rowTitle = messageResources.getMessage("observatory.graphic.mid.puntuation");
            final String columnTitle = messageResources.getMessage("observatory.graphic.verification.points");
            GraphicsUtils.createBarChart(result, title, rowTitle, columnTitle, color, false, false, false, filePath, noDataMess, messageResources, x, y);
        }
    }

    public static void getModalityByVerificationLevelGraphic(final HttpServletRequest request, final List<ObservatoryEvaluationForm> pageExecutionList, final String title, final String filePath, final String noDataMess, final String level, final boolean regenerate) throws Exception {
        final File file = new File(filePath);
        final Map<String, BigDecimal> results = getVerificationResultsByPointAndModality(pageExecutionList, level);
        final MessageResources messageResources = CrawlerUtils.getResources(request);
        final DefaultCategoryDataset dataSet = createStackedBarDataSetForModality(results, messageResources);

        //Incluimos los resultados en la request
        if (level.equals(Constants.OBS_PRIORITY_1)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_GLOBAL_DATA_LIST_MODALITY_VERIFICATION_I, infoLevelVerificationModalityComparison(results));
        } else if (level.equals(Constants.OBS_PRIORITY_2)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_GLOBAL_DATA_LIST_MODALITY_VERIFICATION_II, infoLevelVerificationModalityComparison(results));
        }

        if (!file.exists() || regenerate) {
            final PropertiesManager pmgr = new PropertiesManager();
            final String rowTitle = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.verification.points");
            final String colTitle = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.percentage");
            final ChartForm chartForm = new ChartForm(title, colTitle, rowTitle, dataSet, true, false, false, true, true, false, false, x, y, pmgr.getValue(CRAWLER_PROPERTIES, "chart.observatory.graphic.modality.colors"));
            GraphicsUtils.createStackedBarChart(chartForm, noDataMess, filePath);
        }
    }

    public static List<ModalityComparisonForm> infoLevelVerificationModalityComparison(final Map<String, BigDecimal> results) {
        final List<ModalityComparisonForm> modalityComparisonList = new ArrayList<ModalityComparisonForm>();

        for (String key : results.keySet()) {
            if (!modalityComparisonList.contains(new ModalityComparisonForm(key.replace(Constants.OBS_VALUE_GREEN_SUFFIX, ""))) &&
                    !modalityComparisonList.contains(new ModalityComparisonForm(key.replace(Constants.OBS_VALUE_RED_SUFFIX, "")))) {
                final ModalityComparisonForm modalityComparisonForm = new ModalityComparisonForm();
                if (key.contains(Constants.OBS_VALUE_GREEN_SUFFIX)) {
                    modalityComparisonForm.setVerification(key.replace(Constants.OBS_VALUE_GREEN_SUFFIX, ""));
                    modalityComparisonForm.setGreenPercentage(results.get(key).toString().replace(".00", "") + "%");
                    if (results.get(key.replace(Constants.OBS_VALUE_GREEN_SUFFIX, "") + Constants.OBS_VALUE_RED_SUFFIX) != null) {
                        modalityComparisonForm.setRedPercentage(results.get(key.replace(Constants.OBS_VALUE_GREEN_SUFFIX, "") + Constants.OBS_VALUE_RED_SUFFIX).toString().replace(".00", "") + "%");
                    } else {
                        modalityComparisonForm.setRedPercentage("0%");
                    }
                } else if (key.contains(Constants.OBS_VALUE_RED_SUFFIX)) {
                    modalityComparisonForm.setVerification(key.replace(Constants.OBS_VALUE_RED_SUFFIX, ""));
                    modalityComparisonForm.setRedPercentage(results.get(key).toString().replace(".00", "") + "%");
                    if (results.get(key.replace(Constants.OBS_VALUE_RED_SUFFIX, "") + Constants.OBS_VALUE_GREEN_SUFFIX) != null) {
                        modalityComparisonForm.setGreenPercentage(results.get(key.replace(Constants.OBS_VALUE_RED_SUFFIX, "") + Constants.OBS_VALUE_GREEN_SUFFIX).toString().replace(".00", "") + "%");
                    } else {
                        modalityComparisonForm.setGreenPercentage("0%");
                    }
                }

                modalityComparisonList.add(modalityComparisonForm);
            }
        }

        return modalityComparisonList;
    }

    public static void getGlobalMarkBySegmentsGroupGraphic(final HttpServletRequest request,
                                                           final String filePath, final String noDataMess, final List<ObservatoryEvaluationForm> pageExecutionList, final List<CategoriaForm> categories, final boolean regenerate) throws Exception {
        final String executionId = request.getParameter(Constants.ID);
        final Map<Integer, List<CategoriaForm>> resultLists = createGraphicsMap(categories);
        final List<CategoryViewListForm> categoriesLabels = new ArrayList<CategoryViewListForm>();
        final MessageResources messageResources = CrawlerUtils.getResources(request);

        for (int i = 1; i <= resultLists.size(); i++) {
            final File file = new File(filePath.substring(0, filePath.indexOf(".jpg")) + i + ".jpg");
            final Map<CategoriaForm, Map<String, BigDecimal>> resultsBySegment = calculatePercentageResultsBySegmentMap(executionId, pageExecutionList, resultLists.get(i));
            final DefaultCategoryDataset dataSet = createDataSet(resultsBySegment, messageResources);
            final PropertiesManager pmgr = new PropertiesManager();
            //Si la gráfica no existe, la creamos
            if (!file.exists() || regenerate) {
                final String title = messageResources.getMessage("observatory.graphic.global.puntuation.allocation.segments.mark.title");
                final String rowTitle = messageResources.getMessage("observatory.graphic.puntuation");
                final ChartForm chartForm = new ChartForm(title, "", rowTitle, dataSet, true, false, false, true, true, false, false, x, y, pmgr.getValue(CRAWLER_PROPERTIES, "chart.observatory.graphic.intav.colors"));
                GraphicsUtils.createStackedBarChart(chartForm, noDataMess, filePath.substring(0, filePath.indexOf(".jpg")) + i + ".jpg");
            }

            //Incluimos los resultados en la request
            for (CategoriaForm category : resultLists.get(i)) {
                final CategoryViewListForm categoryView = new CategoryViewListForm(category, infoComparisonBySegmentPuntuation(messageResources, resultsBySegment.get(category)));
                categoriesLabels.add(categoryView);
            }
        }

        request.setAttribute(Constants.OBSERVATORY_GRAPHIC_GLOBAL_DATA_LIST_CPS, categoriesLabels);
        request.setAttribute(Constants.OBSERVATORY_NUM_CPS_GRAPH, resultLists.size());
    }

    private static DefaultCategoryDataset createDataSet(final Map<CategoriaForm, Map<String, BigDecimal>> result, final MessageResources messageResources) {
        final DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
        for (Map.Entry<CategoriaForm, Map<String, BigDecimal>> entry : result.entrySet()) {
            dataSet.addValue(entry.getValue().get(Constants.OBS_NV), GraphicsUtils.parseLevelLabel(Constants.OBS_NV, messageResources), entry.getKey().getName());
            dataSet.addValue(entry.getValue().get(Constants.OBS_A), GraphicsUtils.parseLevelLabel(Constants.OBS_A, messageResources), entry.getKey().getName());
            dataSet.addValue(entry.getValue().get(Constants.OBS_AA), GraphicsUtils.parseLevelLabel(Constants.OBS_AA, messageResources), entry.getKey().getName());
        }

        return dataSet;
    }

    private static DefaultCategoryDataset createStackedBarDataSetForModality(final Map<String, BigDecimal> results, final MessageResources messageResources) {
        final DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
        for (Map.Entry<String, BigDecimal> entry : results.entrySet()) {
            if (entry.getKey().contains(Constants.OBS_VALUE_RED_SUFFIX)) {
                dataSet.addValue(entry.getValue(), messageResources.getMessage("observatory.graphic.modality.red"), entry.getKey().replace(Constants.OBS_VALUE_RED_SUFFIX, "").substring(entry.getKey().replace(Constants.OBS_VALUE_RED_SUFFIX, "").length() - 5));
            } else if (entry.getKey().contains(Constants.OBS_VALUE_GREEN_SUFFIX)) {
                dataSet.addValue(entry.getValue(), messageResources.getMessage("observatory.graphic.modality.green"), entry.getKey().replace(Constants.OBS_VALUE_GREEN_SUFFIX, "").substring(entry.getKey().replace(Constants.OBS_VALUE_GREEN_SUFFIX, "").length() - 5));
            }
        }

        return dataSet;
    }

    public static void getMidMarkAspectEvolutionGraphic(final HttpServletRequest request, final String aspect, final String noDataMess,
                                                        final String filePath, final Map<Date, Map<String, BigDecimal>> resultsByAspect, final String color, final boolean regenerate) throws Exception {
        final MessageResources messageResources = CrawlerUtils.getResources(request);
        final String fileName = filePath + messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.evolution.aspect.mid.puntuation.name", aspect) + ".jpg";
        final File file = new File(fileName);

        final String aspectStr;
        if (aspect.equals(Constants.OBSERVATORY_GRAPHIC_ASPECT_GENERAL_ID)) {
            aspectStr = messageResources.getMessage("observatory.aspect.general");
        } else if (aspect.equals(Constants.OBSERVATORY_GRAPHIC_ASPECT_ALTERNATIVE_ID)) {
            aspectStr = messageResources.getMessage("observatory.aspect.alternatives");
        } else if (aspect.equals(Constants.OBSERVATORY_GRAPHIC_ASPECT_STRUCTURE_ID)) {
            aspectStr = messageResources.getMessage("observatory.aspect.structure");
        } else if (aspect.equals(Constants.OBSERVATORY_GRAPHIC_ASPECT_NAVIGATION_ID)) {
            aspectStr = messageResources.getMessage("observatory.aspect.navigation");
        } else if (aspect.equals(Constants.OBSERVATORY_GRAPHIC_ASPECT_PRESENTATION_ID)) {
            aspectStr = messageResources.getMessage("observatory.aspect.presentation");
        } else {
            aspectStr = "";
        }

        //Recuperamos los resultados
        final Map<String, BigDecimal> resultData = calculateAspectEvolutionPuntuationDataSet(aspectStr, resultsByAspect);
        //Incluimos los resultados en la request
        if (aspect.equals(Constants.OBSERVATORY_GRAPHIC_ASPECT_GENERAL_ID)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_AG, infoMidMarkAspectEvolutionGraphic(messageResources, resultData));
        } else if (aspect.equals(Constants.OBSERVATORY_GRAPHIC_ASPECT_ALTERNATIVE_ID)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_AAL, infoMidMarkAspectEvolutionGraphic(messageResources, resultData));
        } else if (aspect.equals(Constants.OBSERVATORY_GRAPHIC_ASPECT_STRUCTURE_ID)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_AE, infoMidMarkAspectEvolutionGraphic(messageResources, resultData));
        } else if (aspect.equals(Constants.OBSERVATORY_GRAPHIC_ASPECT_NAVIGATION_ID)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_AN, infoMidMarkAspectEvolutionGraphic(messageResources, resultData));
        } else if (aspect.equals(Constants.OBSERVATORY_GRAPHIC_ASPECT_PRESENTATION_ID)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_AP, infoMidMarkAspectEvolutionGraphic(messageResources, resultData));
        }

        //Si no existe la gráfica, la creamos
        if (!file.exists() || regenerate) {
            final String rowTitle = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.mid.puntuation");
            final String columnTitle = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.date");
            final String title = messageResources.getMessage(CrawlerUtils.getLocale(request), "observatory.graphic.evolution.aspect.mid.puntuation", aspectStr);
            GraphicsUtils.createBarChart(resultData, title, rowTitle, columnTitle, color, false, false, true, fileName, noDataMess, messageResources, x, y);
        }
    }

    public static void getMidMarkVerificationEvolutionGraphic(final HttpServletRequest request, final String verification, final String noDataMess,
                                                              final String filePath, final Map<Date, List<ObservatoryEvaluationForm>> result, final String color, final boolean regenerate) throws Exception {
        final MessageResources resources = CrawlerUtils.getResources(request);
        final String fileName = filePath + File.separator + resources.getMessage("observatory.graphic.evolution.verification.mid.puntuation.name", verification) + ".jpg";
        final File file = new File(fileName);

        //Recuperamos los resultados
        final Map<String, BigDecimal> resultData = calculateVerificationEvolutionPuntuationDataSet(verification, result);

        //Los incluimos en la request
        if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_111_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V111, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_112_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V112, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_113_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V113, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_114_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V114, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_115_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V115, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_116_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V116, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_117_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V117, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_121_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V121, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_122_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V122, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_123_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V123, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_124_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V124, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_125_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V125, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_126_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V126, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_211_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V211, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_212_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V212, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_213_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V213, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_214_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V214, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_215_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V215, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_216_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V216, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_217_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V217, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_221_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V221, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_222_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V222, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_223_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V223, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_224_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V224, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_225_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V225, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        } else if (verification.equals(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_226_VERIFICATION)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_V226, infoMidMarkVerificationEvolutionGraphic(resources, resultData));
        }

        //Si no existe la gráfica, la creamos
        if (!file.exists() || regenerate) {
            final String rowTitle = resources.getMessage("observatory.graphic.mid.puntuation");
            final String columnTitle = resources.getMessage("observatory.graphic.date");
            final String title = resources.getMessage("observatory.graphic.evolution.verification.mid.puntuation", verification);
            GraphicsUtils.createBarChart(resultData, title, rowTitle, columnTitle, color, false, false, true, fileName, noDataMess, resources, x, y);
        }
    }

    public static void getMidMarkEvolutionGraphic(final HttpServletRequest request, final String noDataMess, final String filePath,
                                                  final Map<Date, List<ObservatoryEvaluationForm>> observatoryResult, final String color, final boolean regenerate) throws Exception {
        //Recuperamos los resultados
        final Map<String, BigDecimal> resultData = calculateEvolutionPuntuationDataSet(observatoryResult);

        //Los incluimos en la request
        request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_MID_PUNT, infoMidMarkEvolutionGraphic(resultData));

        final File file = new File(filePath);
        //Si no existe la gráfica, la creamos
        if (!file.exists() || regenerate) {
            final MessageResources messageResources = CrawlerUtils.getResources(request);
            final String rowTitle = messageResources.getMessage("observatory.graphic.mid.puntuation");
            final String columnTitle = messageResources.getMessage("observatory.graphic.date");
            final String title = messageResources.getMessage("observatory.graphic.evolution.mid.puntuation");
            GraphicsUtils.createBarChart(resultData, title, rowTitle, columnTitle, color, false, false, true, filePath, noDataMess, messageResources, x, y);
        }

    }

    public static void getApprovalLevelEvolutionGraphic(final HttpServletRequest request, final String type, final String title, final String filePath,
                                                        final String noDataMess, final Map<Date, List<ObservatoryEvaluationForm>> observatoryResult, final String color, final boolean regenerate) throws Exception {
        final File file = new File(filePath);
        final Map<Date, Map<Long, Map<String, Integer>>> result = getEvolutionObservatoriesSitesByType(request, observatoryResult);
        final Map<String, BigDecimal> resultData = calculatePercentageApprovalSiteLevel(result, type);

        //Los incluimos en la request
        if (type.equals(Constants.OBS_A)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_A, infoLevelEvolutionGraphic(resultData));
        } else if (type.equals(Constants.OBS_AA)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_AA, infoLevelEvolutionGraphic(resultData));
        }
        if (type.equals(Constants.OBS_NV)) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_EVOLUTION_DATA_LIST_NV, infoLevelEvolutionGraphic(resultData));
        }

        //Si no existe la gráfica, la creamos
        if (!file.exists() || regenerate) {
            String rowTitle = CrawlerUtils.getResources(request).getMessage("observatory.graphic.pages");
            String columnTitle = CrawlerUtils.getResources(request).getMessage("observatory.graphic.date");
            GraphicsUtils.createBarChart(resultData, title, rowTitle, columnTitle, color, false, true, true, filePath, noDataMess, CrawlerUtils.getResources(request), x, y);
        }

    }

    public static void getMarkAllocationLevelSegmentGraphic(final HttpServletRequest request, final String title, final String filePath,
                                                            final String noDataMess, final List<ObservatorySiteEvaluationForm> siteExecutionList, final boolean showColLab, final boolean regenerate) throws Exception {
        final File file = new File(filePath);
        final List<ObservatorySiteEvaluationForm> result2 = createOrderFormLevel(siteExecutionList);

        //Los incluimos en la request
        if (showColLab) {
            request.setAttribute(Constants.OBSERVATORY_GRAPHIC_SEGMENT_DATA_LIST_DP, infoMarkAllocationLevelSegment(result2));
        }

        //Si no existe la gráfica, la creamos
        if (!file.exists() || regenerate) {
            final String rowTitle = CrawlerUtils.getResources(request).getMessage("observatory.graphic.puntuation");
            final String columnTitle = CrawlerUtils.getResources(request).getMessage("observatory.graphic.portales");
            GraphicsUtils.createBar1PxChart(result2, title, rowTitle, columnTitle, filePath, noDataMess, CrawlerUtils.getResources(request), x, y, showColLab);
        }
    }

    public static void getAspectMidsGraphic(final HttpServletRequest request, final String filePath, final String noDataMess,
                                            final List<ObservatoryEvaluationForm> pageExecutionList, final String color, final String title, final boolean regenerate) throws Exception {
        final Map<String, BigDecimal> result = aspectMidsPuntuationGraphicData(CrawlerUtils.getResources(request), pageExecutionList);

        //Los incluimos en la request
        request.setAttribute(Constants.OBSERVATORY_GRAPHIC_GLOBAL_DATA_LIST_CMA, infoAspectMidsComparison(CrawlerUtils.getResources(request), result));

        //Si no existe la grafica, la creamos
        final File file = new File(filePath);
        if (!file.exists() || regenerate) {
            final String rowTitle = CrawlerUtils.getResources(request).getMessage("observatory.graphic.puntuation");
            final String columnTitle = CrawlerUtils.getResources(request).getMessage("observatory.graphic.aspects");
            GraphicsUtils.createBarChart(result, title, rowTitle, columnTitle, color, false, false, false, filePath, noDataMess, CrawlerUtils.getResources(request), x, y);
        }
    }

    public static List<LabelValueBean> infoMidMarkVerificationEvolutionGraphic(final MessageResources messageResources,
                                                                               final Map<String, BigDecimal> resultData) {
        final List<LabelValueBean> labelValueList = new ArrayList<LabelValueBean>();
        LabelValueBean labelValue;

        for (Map.Entry<String, BigDecimal> entry : resultData.entrySet()) {
            labelValue = new LabelValueBean();
            labelValue.setLabel(entry.getKey());
            if (entry.getValue() != null && entry.getValue().compareTo(new BigDecimal(-1)) != 0) {
                labelValue.setValue(String.valueOf(entry.getValue()));
            } else {
                if (entry.getValue() == null) {
                    labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
                } else {
                    labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
                }
            }
            labelValueList.add(labelValue);
        }

        return labelValueList;
    }

    public static List<LabelValueBean> infoMidMarkAspectEvolutionGraphic(final MessageResources messageResources,
                                                                         final Map<String, BigDecimal> resultData) {
        final List<LabelValueBean> labelValueList = new ArrayList<LabelValueBean>();
        LabelValueBean labelValue;

        for (Map.Entry<String, BigDecimal> entry : resultData.entrySet()) {
            labelValue = new LabelValueBean();
            labelValue.setLabel(entry.getKey());
            if (entry.getValue() != null && entry.getValue().compareTo(new BigDecimal(-1)) != 0) {
                labelValue.setValue(String.valueOf(entry.getValue()));
            } else {
                if (entry.getValue() == null) {
                    labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noResultado"));
                } else {
                    labelValue.setValue(messageResources.getMessage("resultados.observatorio.vista.primaria.valor.noPuntua"));
                }
            }
            labelValueList.add(labelValue);
        }

        return labelValueList;
    }

    public static List<ObservatoryEvaluationForm> getGlobalResultData(final String executionId, final long categoryId, final List<ObservatoryEvaluationForm> pageExecutionList) throws Exception {
        return getGlobalResultData(executionId, categoryId, pageExecutionList, null);
    }

    public static List<ObservatoryEvaluationForm> getGlobalResultData(final String executionId, final long categoryId, final List<ObservatoryEvaluationForm> pageExecutionList, final Long idCrawler) throws Exception {
        List<ObservatoryEvaluationForm> observatoryEvaluationList = null;
        Connection c = null;
        Connection conn = null;
        try {
            observatoryEvaluationList = (List<ObservatoryEvaluationForm>) CacheUtils.getFromCache(Constants.OBSERVATORY_KEY_CACHE + executionId);
        } catch (NeedsRefreshException nre) {
            Logger.putLog("La cache con id " + Constants.OBSERVATORY_KEY_CACHE + executionId + " no está disponible, se va a regenerar", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_INFO);
            try {
                observatoryEvaluationList = new ArrayList<ObservatoryEvaluationForm>();
                final List<Long> listAnalysis = new ArrayList<Long>();

                c = DataBaseManager.getConnection();
                conn = DataBaseManager.getConnection();
                List<Long> listExecutionsIds = new ArrayList<Long>();
                if (idCrawler == null) {
                    listExecutionsIds = RastreoDAO.getExecutionObservatoryCrawlerIds(c, Long.parseLong(executionId), Constants.COMPLEXITY_SEGMENT_NONE);
                } else {
                    listExecutionsIds.add(idCrawler);
                }
                if (pageExecutionList == null) {
                    for (Long idExecution : listExecutionsIds) {
                        listAnalysis.addAll(AnalisisDatos.getAnalysisIdsByTracking(conn, idExecution));
                    }

                    // Inicializamos el evaluador
                    if (!EvaluatorUtility.isInitialized()) {
                        EvaluatorUtility.initialize();
                    }

                    final Evaluator evaluator = new Evaluator();
                    for (Long idAnalysis : listAnalysis) {
                        final Evaluation evaluation = evaluator.getObservatoryAnalisisDB(conn, idAnalysis, EvaluatorUtils.getDocList());
                        final String methodology = ObservatorioDAO.getMethodology(c, Long.parseLong(executionId));
                        final ObservatoryEvaluationForm evaluationForm = EvaluatorUtils.generateObservatoryEvaluationForm(evaluation, methodology, false);
                        evaluationForm.setObservatoryExecutionId(Long.parseLong(executionId));
                        final FulfilledCrawlingForm ffCrawling = RastreoDAO.getFullfilledCrawlingExecution(c, evaluationForm.getCrawlerExecutionId());
                        if (ffCrawling != null) {
                            final SeedForm seedForm = new SeedForm();
                            seedForm.setId(String.valueOf(ffCrawling.getSeed().getId()));
                            seedForm.setAcronym(ffCrawling.getSeed().getAcronimo());
                            seedForm.setName(ffCrawling.getSeed().getNombre());
                            seedForm.setDependence(ffCrawling.getSeed().getDependencia());
                            seedForm.setCategory(ffCrawling.getSeed().getCategoria().getName());
                            evaluationForm.setSeed(seedForm);
                        }
                        observatoryEvaluationList.add(evaluationForm);
                    }
                } else {
                    for (ObservatoryEvaluationForm observatory : pageExecutionList) {
                        if (listExecutionsIds.contains(observatory.getCrawlerExecutionId())) {
                            observatoryEvaluationList.add(observatory);
                        }
                    }
                }
            } catch (Exception e) {
                Logger.putLog("Error en getGlobalResultData", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
                throw e;
            } finally {
                DataBaseManager.closeConnection(c);
                DataBaseManager.closeConnection(conn);
            }
            CacheUtils.putInCacheForever(observatoryEvaluationList, Constants.OBSERVATORY_KEY_CACHE + executionId);
        }

        return filterObservatoriesByComplexity(observatoryEvaluationList, Long.parseLong(executionId), categoryId);
    }

    private static List<ObservatoryEvaluationForm> filterObservatoriesByComplexity(final List<ObservatoryEvaluationForm> observatoryEvaluationList, final Long executionId, final long categoryId)
            throws Exception {
        if (categoryId == Constants.COMPLEXITY_SEGMENT_NONE) {
            return observatoryEvaluationList;
        } else {
            final List<ObservatoryEvaluationForm> results = new ArrayList<ObservatoryEvaluationForm>();
            Connection conn = null;
            try {
                conn = DataBaseManager.getConnection();
                final List<Long> listExecutionsIds = RastreoDAO.getExecutionObservatoryCrawlerIds(conn, executionId, categoryId);
                for (ObservatoryEvaluationForm observatoryEvaluationForm : observatoryEvaluationList) {
                    if (listExecutionsIds.contains(observatoryEvaluationForm.getCrawlerExecutionId())) {
                        results.add(observatoryEvaluationForm);
                    }
                }
            } catch (Exception e) {
                Logger.putLog("Error al filtrar observatorios. ", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
                throw e;
            } finally {
                DataBaseManager.closeConnection(conn);
            }
            return results;
        }
    }

    public static Map<String, BigDecimal> calculateAspectEvolutionPuntuationDataSet(final String aspect, final Map<Date, Map<String, BigDecimal>> resultsByAspect) {
        final Map<String, BigDecimal> resultData = new TreeMap<String, BigDecimal>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                final PropertiesManager pmgr = new PropertiesManager();
                final DateFormat df = new SimpleDateFormat(pmgr.getValue(CRAWLER_PROPERTIES, "date.format.simple.pdf"));
                try {
                    final Date fecha1 = new Date(df.parse(o1).getTime());
                    final Date fecha2 = new Date(df.parse(o2).getTime());
                    return fecha1.compareTo(fecha2);
                } catch (Exception e) {
                    Logger.putLog("Error al ordenar fechas de evolución.", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
                }
                return 0;
            }
        });
        final PropertiesManager pmgr = new PropertiesManager();
        final DateFormat df = new SimpleDateFormat(pmgr.getValue(CRAWLER_PROPERTIES, "date.format.simple.pdf"));

        for (Map.Entry<Date, Map<String, BigDecimal>> entry : resultsByAspect.entrySet()) {
            resultData.put(df.format(entry.getKey()), entry.getValue().get(aspect));
        }

        return resultData;
    }

    public static Map<String, BigDecimal> calculateVerificationEvolutionPuntuationDataSet(final String verification, final Map<Date, List<ObservatoryEvaluationForm>> result) {
        final TreeMap<String, BigDecimal> resultData = new TreeMap<String, BigDecimal>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                final PropertiesManager pmgr = new PropertiesManager();
                final DateFormat df = new SimpleDateFormat(pmgr.getValue(CRAWLER_PROPERTIES, "date.format.simple.pdf"));
                try {
                    final Date fecha1 = new Date(df.parse(o1).getTime());
                    final Date fecha2 = new Date(df.parse(o2).getTime());
                    return fecha1.compareTo(fecha2);
                } catch (Exception e) {
                    Logger.putLog("Error al ordenar fechas de evolución.", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
                }
                return 0;
            }

        });

        final PropertiesManager pmgr = new PropertiesManager();
        final DateFormat df = new SimpleDateFormat(pmgr.getValue(CRAWLER_PROPERTIES, "date.format.simple.pdf"));

        for (Map.Entry<Date, List<ObservatoryEvaluationForm>> entry : result.entrySet()) {
            //Para un observatorio en concreto recuperamos la puntuación de una verificación
            final BigDecimal value = getVerificationResultsByPoint(entry.getValue(), Constants.OBS_PRIORITY_NONE).get(verification);
            resultData.put(df.format(entry.getKey()), value);
        }

        return resultData;
    }

    public static Map<String, BigDecimal> calculateEvolutionPuntuationDataSet(final Map<Date, List<ObservatoryEvaluationForm>> result) {
        final TreeMap<String, BigDecimal> resultData = new TreeMap<String, BigDecimal>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                final PropertiesManager pmgr = new PropertiesManager();
                final DateFormat df = new SimpleDateFormat(pmgr.getValue(CRAWLER_PROPERTIES, "date.format.simple.pdf"));
                try {
                    final Date fecha1 = new Date(df.parse(o1).getTime());
                    final Date fecha2 = new Date(df.parse(o2).getTime());
                    return fecha1.compareTo(fecha2);
                } catch (Exception e) {
                    Logger.putLog("Error al ordenar fechas de evolución. ", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
                }
                return 0;
            }

        });
        final PropertiesManager pmgr = new PropertiesManager();

        for (Map.Entry<Date, List<ObservatoryEvaluationForm>> entry : result.entrySet()) {
            BigDecimal value = BigDecimal.ZERO;
            if ((entry.getValue()) != null && !(entry.getValue()).isEmpty()) {
                for (ObservatoryEvaluationForm observatoryEvaluationForm : entry.getValue()) {
                    value = value.add(observatoryEvaluationForm.getScore());
                }
                final int numPages = entry.getValue().size();
                value = value.divide(new BigDecimal(numPages), 2, BigDecimal.ROUND_HALF_UP);
            }
            final DateFormat df = new SimpleDateFormat(pmgr.getValue(CRAWLER_PROPERTIES, "date.format.simple"));
            resultData.put(df.format(entry.getKey()), value);
        }

        return resultData;
    }

    public static Map<String, BigDecimal> calculatePercentageApprovalSiteLevel(final Map<Date, Map<Long, Map<String, Integer>>> result, final String type) {
        final PropertiesManager pmgr = new PropertiesManager();
        final DateFormat df = new SimpleDateFormat(pmgr.getValue(CRAWLER_PROPERTIES, "date.format.simple"));
        final TreeMap<String, BigDecimal> percentagesMap = new TreeMap<String, BigDecimal>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                final PropertiesManager pmgr = new PropertiesManager();
                final DateFormat df = new SimpleDateFormat(pmgr.getValue(CRAWLER_PROPERTIES, "date.format.simple.pdf"));
                try {
                    final Date fecha1 = new Date(df.parse(o1).getTime());
                    final Date fecha2 = new Date(df.parse(o2).getTime());
                    return fecha1.compareTo(fecha2);
                } catch (Exception e) {
                    Logger.putLog("Error al ordenar fechas de evolución. ", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
                }
                return 0;
            }

        });
        for (Map.Entry<Date, Map<Long, Map<String, Integer>>> dateMapEntry : result.entrySet()) {
            int numSitesType = 0;
            for (Map.Entry<Long, Map<String, Integer>> longMapEntry : dateMapEntry.getValue().entrySet()) {
                final String portalLevel = siteLevel(dateMapEntry.getValue(), longMapEntry.getKey());
                if (portalLevel.equals(type)) {
                    numSitesType++;
                }
            }
            BigDecimal percentage = BigDecimal.ZERO;
            if (numSitesType != 0) {
                final int numSites = dateMapEntry.getValue().size();
                percentage = (new BigDecimal(numSitesType)).divide(new BigDecimal(numSites), 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
            }
            percentagesMap.put(df.format(dateMapEntry.getKey()), percentage);
        }
        return percentagesMap;
    }

    public static Map<Date, Map<Long, Map<String, Integer>>> getEvolutionObservatoriesSitesByType(final HttpServletRequest request, final Map<Date, List<ObservatoryEvaluationForm>> result) {
        final String observatoryId = request.getParameter(Constants.ID_OBSERVATORIO);
        final String executionId = request.getParameter(Constants.ID);
        return getEvolutionObservatoriesSitesByType(observatoryId, executionId, result);
    }

    public static Map<Date, Map<Long, Map<String, Integer>>> getEvolutionObservatoriesSitesByType(final String observatoryId, final String executionId, final Map<Date, List<ObservatoryEvaluationForm>> result) {
        final Map<Date, Map<Long, Map<String, Integer>>> resultData = new HashMap<Date, Map<Long, Map<String, Integer>>>();
        Connection c = null;
        try {
            c = DataBaseManager.getConnection();
            final ObservatorioForm observatoryForm = ObservatorioDAO.getObservatoryForm(c, Long.parseLong(observatoryId));
            final Map<Long, Date> executedObservatoryIdMap = ObservatorioDAO.getObservatoryExecutionIds(c, Long.parseLong(observatoryId), Long.parseLong(executionId), observatoryForm.getCartucho().getId());

            for (Map.Entry<Long, Date> longDateEntry : executedObservatoryIdMap.entrySet()) {
                final List<ObservatoryEvaluationForm> pageList = result.get(longDateEntry.getValue());
                final Map<Long, Map<String, Integer>> sites = getSitesByType(pageList);
                resultData.put(longDateEntry.getValue(), sites);
            }
        } catch (Exception e) {
            Logger.putLog("Exception: ", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
        } finally {
            DataBaseManager.closeConnection(c);
        }
        return resultData;
    }

    public static Map<Date, List<ObservatoryEvaluationForm>> resultEvolutionData(final Long observatoryId, final Long executionId) {
        final Map<Date, List<ObservatoryEvaluationForm>> resultData = new HashMap<Date, List<ObservatoryEvaluationForm>>();

        Connection c = null;
        try {
            c = DataBaseManager.getConnection();
            final ObservatorioForm observatoryForm = ObservatorioDAO.getObservatoryForm(c, observatoryId);
            final Map<Long, Date> executedObservatoryIdMap = ObservatorioDAO.getObservatoryExecutionIds(c, observatoryId, executionId, observatoryForm.getCartucho().getId());
            for (Map.Entry<Long, Date> entry : executedObservatoryIdMap.entrySet()) {
                final List<ObservatoryEvaluationForm> pageList = getGlobalResultData(String.valueOf(entry.getKey()), Constants.COMPLEXITY_SEGMENT_NONE, null);
                resultData.put(entry.getValue(), pageList);
            }
        } catch (Exception e) {
            Logger.putLog("Exception: ", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
        } finally {
            DataBaseManager.closeConnection(c);
        }

        return resultData;
    }

    private static Map<String, BigDecimal> calculatePercentage(final Map<String, Integer> values) {
        int total = 0;
        final Map<String, BigDecimal> result = new HashMap<String, BigDecimal>();
        for (Map.Entry<String, Integer> stringIntegerEntry : values.entrySet()) {
            total += stringIntegerEntry.getValue();
        }
        BigDecimal totalPercentage = BigDecimal.ZERO;
        String fitResultKey = "";
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (total != 0) {
                result.put(entry.getKey(), (new BigDecimal(entry.getValue())).divide(new BigDecimal(total), 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100)));
            } else {
                result.put(entry.getKey(), (BigDecimal.ZERO));
            }
            totalPercentage = totalPercentage.add(result.get(entry.getKey()));
            fitResultKey = entry.getKey();
        }
        //ajustamos el resultado por si se pasa de 100 a causa del redondeo
        if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
            result.put(fitResultKey, result.get(fitResultKey).subtract(totalPercentage.subtract(BigDecimal.valueOf(100))));
        }

        return result;
    }

    public static List<ObservatorySiteEvaluationForm> getSitesListByLevel(final List<ObservatoryEvaluationForm> pages) {
        final List<ObservatorySiteEvaluationForm> siteList = new ArrayList<ObservatorySiteEvaluationForm>();

        try {
            final Map<Long, ObservatorySiteEvaluationForm> siteMap = new HashMap<Long, ObservatorySiteEvaluationForm>();
            for (ObservatoryEvaluationForm page : pages) {
                List<ObservatoryEvaluationForm> pagesL = new ArrayList<ObservatoryEvaluationForm>();
                ObservatorySiteEvaluationForm site = new ObservatorySiteEvaluationForm();
                if (siteMap.get(page.getCrawlerExecutionId()) != null) {
                    site = siteMap.get(page.getCrawlerExecutionId());
                    pagesL = siteMap.get(page.getCrawlerExecutionId()).getPages();
                    site.setScore(site.getScore().add(page.getScore()));
                } else {
                    site.setName(page.getSeed().getName());
                    site.setId(page.getCrawlerExecutionId());
                    site.setScore(page.getScore());
                }
                site.setAcronym(page.getSeed().getAcronym());
                site.setIdSeed(Long.valueOf(page.getSeed().getId()));
                pagesL.add(page);
                site.setPages(pagesL);
                siteMap.put(page.getCrawlerExecutionId(), site);
            }

            for (Map.Entry<Long, ObservatorySiteEvaluationForm> siteEntry : siteMap.entrySet()) {
                final ObservatorySiteEvaluationForm observatorySite = siteEntry.getValue();
                observatorySite.setScore(observatorySite.getScore().divide(new BigDecimal(observatorySite.getPages().size()), 2, BigDecimal.ROUND_HALF_UP));
                observatorySite.setLevel(siteLevel(getSitesByType(observatorySite.getPages()), siteEntry.getKey()));
                observatorySite.setName(siteEntry.getValue().getName());
                siteList.add(siteEntry.getValue());
            }
        } catch (Exception e) {
            Logger.putLog("Exception: ", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
        }

        return siteList;
    }

    private static String siteLevel(final Map<Long, Map<String, Integer>> portalInformation, final Long idSite) {
        final Map<String, Integer> pageType = portalInformation.get(idSite);
        final Integer numPages = pageType.get(Constants.OBS_A) + pageType.get(Constants.OBS_AA) + pageType.get(Constants.OBS_NV);
        final BigDecimal value = ((new BigDecimal(pageType.get(Constants.OBS_A)).multiply(new BigDecimal(5))).add(
                new BigDecimal(pageType.get(Constants.OBS_AA)).multiply(BigDecimal.TEN))).divide(
                new BigDecimal(numPages), 2, BigDecimal.ROUND_HALF_UP);

        if (value.compareTo(new BigDecimal(8)) >= 0) {
            return Constants.OBS_AA;
        } else if (value.compareTo(new BigDecimal("3.5")) <= 0) {
            return Constants.OBS_NV;
        } else {
            return Constants.OBS_A;
        }
    }

    public static void getGlobalAccessibilityLevelAllocationSegmentGraphic(final HttpServletRequest request, final List<ObservatoryEvaluationForm> pageExecutionList, final String title, final String filePath, final String noDataMess, final boolean regenerate) throws Exception {
        final PropertiesManager pmgr = new PropertiesManager();
        final File file = new File(filePath);
        final Map<String, Integer> result = getResultsBySiteLevel(pageExecutionList);

        final MessageResources messageResources = CrawlerUtils.getResources(request);
        if (!file.exists() || regenerate) {
            GraphicsUtils.totalPageStr = messageResources.getMessage("observatory.graphic.site.number");
            GraphicsUtils.totalPage = result.get(Constants.OBS_A) + result.get(Constants.OBS_AA) + result.get(Constants.OBS_NV);

            final DefaultPieDataset dataSet = new DefaultPieDataset();

            dataSet.setValue(GraphicsUtils.parseLevelLabel(Constants.OBS_PARCIAL, messageResources), result.get(Constants.OBS_NV));
            dataSet.setValue(GraphicsUtils.parseLevelLabel(Constants.OBS_A, messageResources), result.get(Constants.OBS_A));
            dataSet.setValue(GraphicsUtils.parseLevelLabel(Constants.OBS_AA, messageResources), result.get(Constants.OBS_AA));

            GraphicsUtils.createPieChart(dataSet, title, filePath, noDataMess, pmgr.getValue(CRAWLER_PROPERTIES, "chart.observatory.graphic.intav.colors"), x, y);
        }
        request.setAttribute(Constants.OBSERVATORY_GRAPHIC_GLOBAL_DATA_LIST_DAG, infoGlobalAccessibilityLevel(messageResources, result));
    }

    public static Map<Integer, List<CategoriaForm>> createGraphicsMap(final List<CategoriaForm> categories) {
        final Map<Integer, List<CategoriaForm>> resultLists = new TreeMap<Integer, List<CategoriaForm>>();
        final PropertiesManager pmgr = new PropertiesManager();
        final int numBarByGrapg = Integer.parseInt(pmgr.getValue(CRAWLER_PROPERTIES, "num.max.bar.graph"));
        int keyMap = 1;
        for (int i = 1; i <= categories.size(); i++) {
            final List<CategoriaForm> list;
            if (resultLists.get(keyMap) != null) {
                list = resultLists.get(keyMap);
            } else {
                list = new ArrayList<CategoriaForm>();
            }
            list.add(categories.get(i - 1));
            resultLists.put(keyMap, list);
            if ((i >= numBarByGrapg) && (i % numBarByGrapg == 0) && (categories.size() != i + 1)) {
                keyMap++;
            }
        }
        return resultLists;
    }

    public static void getGlobalMarkBySegmentGraphic(final HttpServletRequest request, final List<ObservatoryEvaluationForm> pageExecutionList,
                                                     final String title, final String filePath, final String noDataMess, final List<CategoriaForm> categories) throws Exception {
        final PropertiesManager pmgr = new PropertiesManager();
        final String executionId = request.getParameter(Constants.ID);
        final Map<Integer, List<CategoriaForm>> resultLists = createGraphicsMap(categories);
        final List<CategoryViewListForm> categoriesLabels = new ArrayList<CategoryViewListForm>();
        final MessageResources messageResources = CrawlerUtils.getResources(request);

        for (int i = 1; i <= resultLists.size(); i++) {
            final File file = new File(filePath.substring(0, filePath.indexOf(".jpg")) + i + ".jpg");
            final Map<CategoriaForm, Map<String, BigDecimal>> resultDataBySegment = calculateMidPuntuationResultsBySegmentMap(executionId, pageExecutionList, resultLists.get(i));
            if (!file.exists()) {
                final String rowTitle = messageResources.getMessage("observatory.graphic.puntuation");
                final ChartForm observatoryGraphicsForm = new ChartForm(title, "", rowTitle, createDataSet(resultDataBySegment, messageResources), true, true, false, false, true, false, false, x, y, pmgr.getValue(CRAWLER_PROPERTIES, "chart.observatory.graphic.intav.colors"));
                GraphicsUtils.createSeriesBarChart(observatoryGraphicsForm, filePath.substring(0, filePath.indexOf(".jpg")) + i + ".jpg", noDataMess, messageResources, true);
            }
            for (CategoriaForm category : resultLists.get(i)) {
                final CategoryViewListForm categoryView = new CategoryViewListForm(category, infoComparisonBySegment(messageResources, resultDataBySegment.get(category)));
                categoriesLabels.add(categoryView);
            }
        }
        request.setAttribute(Constants.OBSERVATORY_GRAPHIC_GLOBAL_DATA_LIST_CAS, categoriesLabels);
        request.setAttribute(Constants.OBSERVATORY_NUM_CAS_GRAPH, resultLists.size());
    }

    public static Map<String, Integer> getResultsBySiteLevel(final List<ObservatoryEvaluationForm> observatoryEvaluationList) {
        final Map<String, Integer> globalResult = new HashMap<String, Integer>();
        globalResult.put(Constants.OBS_NV, 0);
        globalResult.put(Constants.OBS_A, 0);
        globalResult.put(Constants.OBS_AA, 0);

        final Map<Long, Map<String, Integer>> globalResultBySiteType = getSitesByType(observatoryEvaluationList);

        for (Long idSite : globalResultBySiteType.keySet()) {
            final Map<String, Integer> pageType = globalResultBySiteType.get(idSite);
            final Integer numPages = pageType.get(Constants.OBS_A) + pageType.get(Constants.OBS_AA) + pageType.get(Constants.OBS_NV);
            final BigDecimal value = ((new BigDecimal(pageType.get(Constants.OBS_A)).multiply(new BigDecimal(5))).add(
                    new BigDecimal(pageType.get(Constants.OBS_AA)).multiply(BigDecimal.TEN))).divide(
                    new BigDecimal(numPages), 2, BigDecimal.ROUND_HALF_UP);
            if (value.compareTo(new BigDecimal(8)) >= 0) {
                globalResult.put(Constants.OBS_AA, globalResult.get(Constants.OBS_AA) + 1);
            } else if (value.compareTo(new BigDecimal("3.5")) <= 0) {
                globalResult.put(Constants.OBS_NV, globalResult.get(Constants.OBS_NV) + 1);
            } else {
                globalResult.put(Constants.OBS_A, globalResult.get(Constants.OBS_A) + 1);
            }
        }

        return globalResult;
    }

    public static Map<String, BigDecimal> aspectMidsPuntuationGraphicData(final MessageResources messageResources, final List<ObservatoryEvaluationForm> resultData) {
        final Map<String, List<LabelValueBean>> globalResult = new HashMap<String, List<LabelValueBean>>();
        for (ObservatoryEvaluationForm observatoryEvaluationForm : resultData) {
            for (ObservatoryLevelForm levelForm : observatoryEvaluationForm.getGroups()) {
                for (ObservatorySuitabilityForm suitabilityForm : levelForm.getSuitabilityGroups()) {
                    for (ObservatorySubgroupForm subgroupForm : suitabilityForm.getSubgroups()) {
                        if (!globalResult.containsKey(subgroupForm.getAspect())) {
                            globalResult.put(subgroupForm.getAspect(), new ArrayList<LabelValueBean>());
                        }
                        LabelValueBean lvb = new LabelValueBean();
                        lvb.setLabel(subgroupForm.getDescription());
                        lvb.setValue(String.valueOf(subgroupForm.getValue()));
                        globalResult.get(subgroupForm.getAspect()).add(lvb);
                    }
                }
            }
        }

        final Map<String, BigDecimal> results = new HashMap<String, BigDecimal>();
        for (Map.Entry<String, List<LabelValueBean>> globalResultEntry : globalResult.entrySet()) {
            final String aspect = messageResources.getMessage(globalResultEntry.getKey());
            // Recorremos las verificaciones de cada aspecto
            final Map<String, List<BigDecimal>> partialResultsMap = new HashMap<String, List<BigDecimal>>();
            for (LabelValueBean lvb : globalResultEntry.getValue()) {
                int value = Integer.parseInt(lvb.getValue());
                if (!partialResultsMap.containsKey(lvb.getLabel())) {
                    partialResultsMap.put(lvb.getLabel(), new ArrayList<BigDecimal>());
                }
                if (value == Constants.OBS_VALUE_GREEN_ONE) {
                    partialResultsMap.get(lvb.getLabel()).add(BigDecimal.ONE);
                } else if (value == Constants.OBS_VALUE_GREEN_ZERO || value == Constants.OBS_VALUE_RED_ZERO) {
                    partialResultsMap.get(lvb.getLabel()).add(BigDecimal.ZERO);
                }
            }

            // Hemos recorrido las verificaciones del aspecto, ahora calculamos la media de cada una
            final Map<String, BigDecimal> verificationsMap = new HashMap<String, BigDecimal>();
            for (String verificationKey : partialResultsMap.keySet()) {
                List<BigDecimal> verificationsList = partialResultsMap.get(verificationKey);
                if (!verificationsList.isEmpty()) {
                    if (!verificationsMap.containsKey(verificationKey)) {
                        verificationsMap.put(verificationKey, BigDecimal.ZERO);
                    }
                    for (BigDecimal value : verificationsList) {
                        verificationsMap.put(verificationKey, verificationsMap.get(verificationKey).add(value));
                    }
                    verificationsMap.put(verificationKey, verificationsMap.get(verificationKey).divide(new BigDecimal(verificationsList.size()), 2, BigDecimal.ROUND_HALF_UP));
                }
            }

            // Teniendo las medias de verificaciones de un aspecto, pasamos a calcular el valor medio de ese aspecto
            if (!results.containsKey(aspect)) {
                results.put(aspect, BigDecimal.ZERO);
            }
            for (String verificationKey : verificationsMap.keySet()) {
                results.put(aspect, results.get(aspect).add(verificationsMap.get(verificationKey)));
            }

            if (verificationsMap.size() > 0) {
                results.put(aspect, results.get(aspect).divide(new BigDecimal(verificationsMap.size()), 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.TEN));
            }
        }

        return results;
    }

    public static Map<String, BigDecimal> barGraphicFromMidPuntuationSegmentData(final List<ObservatorySiteEvaluationForm> categoryList) {
        final Map<String, BigDecimal> globalResult = new HashMap<String, BigDecimal>();

        BigDecimal countA = BigDecimal.ZERO;
        BigDecimal countAA = BigDecimal.ZERO;
        BigDecimal countNV = BigDecimal.ZERO;

        globalResult.put(Constants.OBS_NV, BigDecimal.ZERO);
        globalResult.put(Constants.OBS_A, BigDecimal.ZERO);
        globalResult.put(Constants.OBS_AA, BigDecimal.ZERO);

        for (ObservatorySiteEvaluationForm observatorySite : categoryList) {
            if (observatorySite.getLevel().equals(Constants.OBS_A)) {
                countA = countA.add(BigDecimal.ONE);
                globalResult.put(Constants.OBS_A, globalResult.get(Constants.OBS_A).add(observatorySite.getScore()));
            }
            if (observatorySite.getLevel().equals(Constants.OBS_AA)) {
                countAA = countAA.add(BigDecimal.ONE);
                globalResult.put(Constants.OBS_AA, globalResult.get(Constants.OBS_AA).add(observatorySite.getScore()));
            }
            if (observatorySite.getLevel().equals(Constants.OBS_NV)) {
                countNV = countNV.add(BigDecimal.ONE);
                globalResult.put(Constants.OBS_NV, globalResult.get(Constants.OBS_NV).add(observatorySite.getScore()));
            }
        }

        if (!countA.equals(BigDecimal.ZERO)) {
            globalResult.put(Constants.OBS_A, globalResult.get(Constants.OBS_A).divide(countA, 2, BigDecimal.ROUND_HALF_UP));
        }
        if (!countAA.equals(BigDecimal.ZERO)) {
            globalResult.put(Constants.OBS_AA, globalResult.get(Constants.OBS_AA).divide(countAA, 2, BigDecimal.ROUND_HALF_UP));
        }
        if (!countNV.equals(BigDecimal.ZERO)) {
            globalResult.put(Constants.OBS_NV, globalResult.get(Constants.OBS_NV).divide(countNV, 2, BigDecimal.ROUND_HALF_UP));
        }

        return globalResult;
    }

    public static Map<String, BigDecimal> getVerificationResultsByPoint(final List<ObservatoryEvaluationForm> resultData, final String level) {
        final Map<String, Integer> results = new TreeMap<String, Integer>();
        final Map<String, Integer> numPoint = new HashMap<String, Integer>();

        for (ObservatoryEvaluationForm observatoryEvaluationForm : resultData) {
            for (ObservatoryLevelForm observatoryLevelForm : observatoryEvaluationForm.getGroups()) {
                if (level.equals(Constants.OBS_PRIORITY_NONE) || observatoryLevelForm.getName().equals(level)) {
                    for (ObservatorySuitabilityForm observatorySuitabilityForm : observatoryLevelForm.getSuitabilityGroups()) {
                        for (ObservatorySubgroupForm observatorySubgroupForm : observatorySuitabilityForm.getSubgroups()) {
                            //Se comprueba si puntúa o no puntúa
                            if (observatorySubgroupForm.getValue() != Constants.OBS_VALUE_NOT_SCORE) {
                                //Si puntúa, se isNombreValido si se le da un 0 o un 1
                                if (observatorySubgroupForm.getValue() == Constants.OBS_VALUE_GREEN_ONE) {
                                    //Si le damos un 1, lo añadimos a la puntuación e incrementamos el número
                                    //de puntos que han puntuado
                                    if (results.get(observatorySubgroupForm.getDescription()) == null) {
                                        results.put(observatorySubgroupForm.getDescription(), 1);
                                        numPoint.put(observatorySubgroupForm.getDescription(), 1);
                                    } else {
                                        results.put(observatorySubgroupForm.getDescription(), results.get(observatorySubgroupForm.getDescription()) + 1);
                                        if (numPoint.get(observatorySubgroupForm.getDescription()) == -1) {
                                            numPoint.put(observatorySubgroupForm.getDescription(), numPoint.get(observatorySubgroupForm.getDescription()) + 2);
                                        } else {
                                            numPoint.put(observatorySubgroupForm.getDescription(), numPoint.get(observatorySubgroupForm.getDescription()) + 1);
                                        }
                                    }
                                } else {
                                    //Si le damos un 0 solamente incrementamos el número de puntos
                                    if (results.get(observatorySubgroupForm.getDescription()) == null) {
                                        results.put(observatorySubgroupForm.getDescription(), 0);
                                        numPoint.put(observatorySubgroupForm.getDescription(), 1);
                                    } else {
                                        numPoint.put(observatorySubgroupForm.getDescription(), numPoint.get(observatorySubgroupForm.getDescription()) + 1);
                                    }
                                }
                            } else {
                                if (results.get(observatorySubgroupForm.getDescription()) == null) {
                                    results.put(observatorySubgroupForm.getDescription(), 0);
                                    numPoint.put(observatorySubgroupForm.getDescription(), -1);
                                }
                            }
                        }
                    }
                }
            }
        }
        //Cambiamos las claves  por el nombre y calculamos la media
        final Map<String, BigDecimal> verificationResultsByPoint = new TreeMap<String, BigDecimal>();
        for (Map.Entry<String, Integer> resultEntry : results.entrySet()) {
            final String name = resultEntry.getKey().substring(resultEntry.getKey().length() - 5);
            final BigDecimal value;
            if (numPoint.get(resultEntry.getKey()) != -1 && numPoint.get(resultEntry.getKey()) != 0) {
                value = BigDecimal.valueOf(resultEntry.getValue()).divide(BigDecimal.valueOf(numPoint.get(resultEntry.getKey())), 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.TEN);
            } else if (numPoint.get(resultEntry.getKey()) == -1) {
                value = BigDecimal.valueOf(-1);
            } else {
                value = BigDecimal.ZERO;
            }
            verificationResultsByPoint.put(name, value);
        }
        return verificationResultsByPoint;
    }

    public static Map<String, BigDecimal> getVerificationResultsByPointAndModality(final List<ObservatoryEvaluationForm> resultData, final String level) {
        final Map<String, BigDecimal> results = new TreeMap<String, BigDecimal>();
        for (ObservatoryEvaluationForm observatoryEvaluationForm : resultData) {
            for (ObservatoryLevelForm observatoryLevelForm : observatoryEvaluationForm.getGroups()) {
                if (level.equals(Constants.OBS_PRIORITY_NONE) || observatoryLevelForm.getName().equals(level)) {
                    for (ObservatorySuitabilityForm observatorySuitabilityForm : observatoryLevelForm.getSuitabilityGroups()) {
                        for (ObservatorySubgroupForm observatorySubgroupForm : observatorySuitabilityForm.getSubgroups()) {
                            if (observatorySubgroupForm.getValue() == Constants.OBS_VALUE_GREEN_ONE
                                    || observatorySubgroupForm.getValue() == Constants.OBS_VALUE_GREEN_ZERO
                                    || observatorySubgroupForm.getValue() == Constants.OBS_VALUE_NOT_SCORE) {
                                if (results.containsKey(observatorySubgroupForm.getDescription() + Constants.OBS_VALUE_GREEN_SUFFIX)) {
                                    results.put(observatorySubgroupForm.getDescription() + Constants.OBS_VALUE_GREEN_SUFFIX, results.get(observatorySubgroupForm.getDescription() + Constants.OBS_VALUE_GREEN_SUFFIX).add(BigDecimal.ONE));
                                } else {
                                    results.put(observatorySubgroupForm.getDescription() + Constants.OBS_VALUE_GREEN_SUFFIX, BigDecimal.ONE);
                                }
                            } else if (observatorySubgroupForm.getValue() == Constants.OBS_VALUE_RED_ZERO) {
                                if (results.containsKey(observatorySubgroupForm.getDescription() + Constants.OBS_VALUE_RED_SUFFIX)) {
                                    results.put(observatorySubgroupForm.getDescription() + Constants.OBS_VALUE_RED_SUFFIX, results.get(observatorySubgroupForm.getDescription() + Constants.OBS_VALUE_RED_SUFFIX).add(BigDecimal.ONE));
                                } else {
                                    results.put(observatorySubgroupForm.getDescription() + Constants.OBS_VALUE_RED_SUFFIX, BigDecimal.ONE);
                                }
                            }
                        }
                    }
                }
            }
        }


        for (Map.Entry<String, BigDecimal> stringBigDecimalEntry : results.entrySet()) {
            results.put(stringBigDecimalEntry.getKey(), stringBigDecimalEntry.getValue().divide(new BigDecimal(resultData.size()), 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100)));
        }

        return results;
    }

    public static List<ObservatorySiteEvaluationForm> createOrderFormLevel(final List<ObservatorySiteEvaluationForm> results) {
        Collections.sort(results, new Comparator<ObservatorySiteEvaluationForm>() {
            @Override
            public int compare(ObservatorySiteEvaluationForm o1,
                               ObservatorySiteEvaluationForm o2) {
                return o2.getScore().compareTo(o1.getScore());
            }
        });

        return results;
    }

    public static Map<String, List<ObservatoryEvaluationForm>> getPagesByType(final List<ObservatoryEvaluationForm> observatoryEvaluationList) {
        final Map<String, List<ObservatoryEvaluationForm>> globalResult = new HashMap<String, List<ObservatoryEvaluationForm>>();
        globalResult.put(Constants.OBS_NV, new ArrayList<ObservatoryEvaluationForm>());
        globalResult.put(Constants.OBS_A, new ArrayList<ObservatoryEvaluationForm>());
        globalResult.put(Constants.OBS_AA, new ArrayList<ObservatoryEvaluationForm>());

        final PropertiesManager pmgr = new PropertiesManager();
        final int maxFails = Integer.parseInt(pmgr.getValue("intav.properties", "observatory.zero.red.max.number"));

        //Se recorren las páginas de cada observatorio
        for (ObservatoryEvaluationForm observatoryEvaluationForm : observatoryEvaluationList) {

            boolean isA = true;
            boolean isAA = true;

            //Se recorren los niveles de análisis
            for (ObservatoryLevelForm observatoryLevel : observatoryEvaluationForm.getGroups()) {
                //Se recorren los niveles de acecuación
                for (ObservatorySuitabilityForm observatorySuitabilityForm : observatoryLevel.getSuitabilityGroups()) {
                    int numZeroRed = 0;
                    if (observatorySuitabilityForm.getName().equals(Constants.OBS_A)) {
                        if ((observatoryLevel.getName().equals(Constants.OBS_N1)) || (isA)) {
                            for (ObservatorySubgroupForm observatorySubgroupForm : observatorySuitabilityForm.getSubgroups()) {
                                if (observatorySubgroupForm.getValue() == Constants.OBS_VALUE_RED_ZERO) {
                                    numZeroRed = numZeroRed + 1;
                                }
                            }
                            if (numZeroRed > maxFails) {
                                isA = false;
                            }
                        }
                    } else if (observatorySuitabilityForm.getName().equals(Constants.OBS_AA) && isA) {
                        if ((observatoryLevel.getName().equals(Constants.OBS_N1)) || (isAA)) {
                            for (ObservatorySubgroupForm observatorySubgroupForm : observatorySuitabilityForm.getSubgroups()) {
                                if (observatorySubgroupForm.getValue() == Constants.OBS_VALUE_RED_ZERO) {
                                    numZeroRed = numZeroRed + 1;
                                }
                            }
                            if (numZeroRed > maxFails) {
                                isAA = false;
                            }
                        }
                    }
                }
            }

            if (isA && isAA) {
                final List<ObservatoryEvaluationForm> globalResult2 = globalResult.get(Constants.OBS_AA);
                globalResult2.add(observatoryEvaluationForm);
                globalResult.put(Constants.OBS_AA, globalResult2);
            } else if (isA) {
                final List<ObservatoryEvaluationForm> globalResult2 = globalResult.get(Constants.OBS_A);
                globalResult2.add(observatoryEvaluationForm);
                globalResult.put(Constants.OBS_A, globalResult2);
            } else {
                final List<ObservatoryEvaluationForm> globalResult2 = globalResult.get(Constants.OBS_NV);
                globalResult2.add(observatoryEvaluationForm);
                globalResult.put(Constants.OBS_NV, globalResult2);
            }
        }

        return globalResult;
    }

    private static Map<Long, Map<String, Integer>> getSitesByType(final List<ObservatoryEvaluationForm> observatoryEvaluationList) {
        final Map<String, List<ObservatoryEvaluationForm>> pagesByType = getPagesByType(observatoryEvaluationList);
        final Map<Long, Map<String, Integer>> sitesByType = new HashMap<Long, Map<String, Integer>>();

        for (String key : pagesByType.keySet()) {
            for (ObservatoryEvaluationForm observatoryEvaluationForm : pagesByType.get(key)) {
                if (sitesByType.get(observatoryEvaluationForm.getCrawlerExecutionId()) != null) {
                    final Map<String, Integer> value = sitesByType.get(observatoryEvaluationForm.getCrawlerExecutionId());
                    value.put(key, value.get(key) + 1);
                    sitesByType.put(observatoryEvaluationForm.getCrawlerExecutionId(), value);
                } else {
                    final Map<String, Integer> initialValues = new HashMap<String, Integer>();
                    if (key.equals(Constants.OBS_NV)) {
                        initialValues.put(Constants.OBS_NV, 1);
                    } else {
                        initialValues.put(Constants.OBS_NV, 0);
                    }
                    if (key.equals(Constants.OBS_A)) {
                        initialValues.put(Constants.OBS_A, 1);
                    } else {
                        initialValues.put(Constants.OBS_A, 0);
                    }
                    if (key.equals(Constants.OBS_AA)) {
                        initialValues.put(Constants.OBS_AA, 1);
                    } else {
                        initialValues.put(Constants.OBS_AA, 0);
                    }
                    sitesByType.put(observatoryEvaluationForm.getCrawlerExecutionId(), initialValues);
                }
            }
        }

        return sitesByType;
    }

    //Cálculo de resultados
    public static Map<CategoriaForm, Map<String, BigDecimal>> calculatePercentageResultsBySegmentMap(final String executionId, final List<ObservatoryEvaluationForm> pageExecutionList, final List<CategoriaForm> categories) throws Exception {
        final Map<CategoriaForm, Map<String, BigDecimal>> resultsBySegment = new TreeMap<CategoriaForm, Map<String, BigDecimal>>(new Comparator<CategoriaForm>() {
            @Override
            public int compare(CategoriaForm o1, CategoriaForm o2) {
                return (Long.valueOf(o1.getId()).compareTo(Long.valueOf(o2.getId())));
            }
        });

        for (CategoriaForm category : categories) {
            final List<ObservatoryEvaluationForm> resultDataSegment = getGlobalResultData(executionId, Long.parseLong(category.getId()), pageExecutionList);
            resultsBySegment.put(category, calculatePercentage(getResultsBySiteLevel(resultDataSegment)));
        }
        return resultsBySegment;
    }

    public static Map<CategoriaForm, Map<String, BigDecimal>> calculateMidPuntuationResultsBySegmentMap(final String executionId, final List<ObservatoryEvaluationForm> pageExecutionList, final List<CategoriaForm> categories) throws Exception {
        Connection conn = null;
        try {
            conn = DataBaseManager.getConnection();

            final Map<CategoriaForm, Map<String, BigDecimal>> resultDataBySegment = new TreeMap<CategoriaForm, Map<String, BigDecimal>>(new Comparator<CategoriaForm>() {
                @Override
                public int compare(CategoriaForm o1, CategoriaForm o2) {
                    return (Long.valueOf(o1.getId()).compareTo(Long.valueOf(o2.getId())));
                }
            });
            for (CategoriaForm category : categories) {
                final List<ObservatorySiteEvaluationForm> categoryList = getSitesListByLevel(getGlobalResultData(executionId, Long.parseLong(category.getId()), pageExecutionList));
                resultDataBySegment.put(category, barGraphicFromMidPuntuationSegmentData(categoryList));
            }

            return resultDataBySegment;
        } catch (Exception e) {
            Logger.putLog("Error al recuperar datos de la BBDD.", ResultadosAnonimosObservatorioUNE2012Utils.class, Logger.LOG_LEVEL_ERROR, e);
            throw e;
        } finally {
            DataBaseManager.closeConnection(conn);
        }
    }

}