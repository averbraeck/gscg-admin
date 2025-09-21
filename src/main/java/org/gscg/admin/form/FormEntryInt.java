package org.gscg.admin.form;

public class FormEntryInt extends AbstractFormEntry<FormEntryInt, Integer>
{

    int min;

    int max;

    int step;

    String pattern;

    public FormEntryInt(final String label, final String name)
    {
        super(label, name);
        this.min = -Integer.MAX_VALUE;
        this.max = Integer.MAX_VALUE;
        this.step = 1;
        this.pattern = "\\d+";
    }

    public int getMin()
    {
        return this.min;
    }

    public FormEntryInt setMin(final int min)
    {
        this.min = min;
        return this;
    }

    public int getMax()
    {
        return this.max;
    }

    public FormEntryInt setMax(final int max)
    {
        this.max = max;
        return this;
    }

    public int getStep()
    {
        return this.step;
    }

    public FormEntryInt setStep(final int step)
    {
        this.step = step;
        return this;
    }

    public String getPattern()
    {
        return this.pattern;
    }

    public FormEntryInt setPattern(final String pattern)
    {
        this.pattern = pattern;
        return this;
    }

    @Override
    public String codeForEdit(final Integer value)
    {
        if (value == null)
            return "";
        return value.toString();
    }

    @Override
    public Integer codeForType(final String s)
    {
        return Integer.valueOf(s);
    }

    @Override
    protected void validate(final String value)
    {
        super.validate(value);
        try
        {
            int v = Integer.valueOf(value);
            if (v < getMin())
                addError("Value lower than minimum " + getMin());
            if (v > getMax())
                addError("Value larger than maximum " + getMax());
            // TODO: step, pattern
        }
        catch (Exception exception)
        {
            addError("Exception: " + exception.getMessage());
        }
    }

    @Override
    public String makeHtml()
    {
        StringBuilder s = new StringBuilder();

        if (isHidden())
        {
            s.append("    <input type=\"hidden\" name=\"");
            s.append(getName());
            s.append("\" value=\"");
            s.append(getLastEnteredValue() == null ? "" : getLastEnteredValue());
            s.append("\" />\n");
            return s.toString();
        }

        s.append("    <tr>\n");
        String labelLength = getForm() == null ? "25%" : getForm().getLabelLength();
        s.append("      <td width=\"" + labelLength + "\">");
        s.append(getLabel());
        if (isRequired())
            s.append(" *");
        s.append("      </td>");
        String fieldLength = getForm() == null ? "75%" : getForm().getFieldLength();
        s.append("      <td width=\"" + fieldLength + "\">");
        s.append("<input type=\"number\" min=\"");
        s.append(getMin());
        s.append("\" max=\"");
        s.append(getMax());
        s.append("\" step=\"");
        s.append(getStep());
        s.append("\" pattern=\"");
        s.append(getPattern());
        if (isRequired())
            s.append("\" required name=\"");
        else
            s.append("\" name=\"");
        s.append(getName());
        s.append("\" value=\"");
        s.append(getLastEnteredValue() == null ? "" : getLastEnteredValue());
        if (isReadOnly())
            s.append("\" readonly />");
        else
            s.append("\" />");
        s.append("</td>\n");
        s.append("    </tr>\n");
        return s.toString();
    }

}
