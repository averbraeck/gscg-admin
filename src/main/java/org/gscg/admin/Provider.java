package org.gscg.admin;

/**
 * IdProvider.java.
 * <p>
 * Copyright (c) 2020-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class Provider
{
    public static <R extends org.jooq.Record> int getId(final R record)
    {
        if (record.get("id") == null)
            return -1;
        return Integer.valueOf(record.get("id").toString());
    }

    public static <R extends org.jooq.Record> String getName(final R record, final String nameField)
    {
        return record.get(record.field(nameField)).toString();
    }
}
