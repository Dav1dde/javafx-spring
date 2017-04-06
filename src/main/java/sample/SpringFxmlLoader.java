package sample;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;


@Component
public class SpringFxmlLoader {

    private final ApplicationContext applicationContext;
    private final ResourceLoader resourceLoader;

    public SpringFxmlLoader(ApplicationContext applicationContext, ResourceLoader resourceLoader) {
        this.applicationContext = applicationContext;
        this.resourceLoader = resourceLoader;
    }

    private FXMLLoader generateFXMLLoader(String url) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(this.resourceLoader.getResource(url).getURL());
        fxmlLoader.setControllerFactory(applicationContext::getBean);
        return fxmlLoader;
    }

    private Object postProcessFields(FXMLLoader fxmlLoader) throws Exception
    {
        Object controller = fxmlLoader.getController();

        if (AopUtils.isAopProxy(controller) && controller instanceof Advised)
        {
            Object targetController = ((Advised) controller).getTargetSource().getTarget();

            // copy over every field which was set on the proxy to the actual controller instance
            // just to be safe we only copy @FXML annotated and not static fields
            ReflectionUtils.doWithFields(targetController.getClass(), f -> {
                ReflectionUtils.makeAccessible(f);
                ReflectionUtils.setField(f, targetController, ReflectionUtils.getField(f, controller));
            }, f -> !Modifier.isStatic(f.getModifiers()) && f.isAnnotationPresent(FXML.class));
        }

        return controller;
    }

    public Object load(String url) {
        FXMLLoader fxmlLoader;
        try {
            fxmlLoader = generateFXMLLoader(url);
            Object result = fxmlLoader.load();
            postProcessFields(fxmlLoader);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}