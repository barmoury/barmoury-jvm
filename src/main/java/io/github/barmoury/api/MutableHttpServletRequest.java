package io.github.barmoury.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String[]> mutableParams = new HashMap<>();

    public MutableHttpServletRequest(final HttpServletRequest request) {
        super(request);
    }

    // parameters

    public MutableHttpServletRequest addParameter(String name, String value) {
        if (value == null) return this;
        String[] existingValues = mutableParams.containsKey(name) ? mutableParams.get(name) : new String[]{};
        String[] newValues = Arrays.copyOf(existingValues, existingValues.length + 1);
        newValues[existingValues.length] = value;
        mutableParams.put(name, newValues);
        return this;
    }

    public MutableHttpServletRequest addParameters(String name, String... value) {
        if (value != null) mutableParams.put(name, value);
        return this;
    }

    @Override
    public String getParameter(final String name) {
        String[] values = getParameterMap().get(name);
        if (values == null) values = new String[0];

        return Arrays.stream(values)
                .findFirst()
                .orElse(super.getParameter(name));
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> allParameters = new HashMap<>();
        allParameters.putAll(super.getParameterMap());
        allParameters.putAll(mutableParams);

        return Collections.unmodifiableMap(allParameters);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(getParameterMap().keySet());
    }

    @Override
    public String[] getParameterValues(final String name) {
        return getParameterMap().get(name);
    }

}
