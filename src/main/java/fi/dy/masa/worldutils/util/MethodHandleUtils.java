package fi.dy.masa.worldutils.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class MethodHandleUtils
{
    public static class UnableToFindMethodHandleException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public UnableToFindMethodHandleException(String[] methodNames, Exception failed)
        {
            super(failed);
        }

    }

    public static <E> MethodHandle getMethodHandleVirtual(Class<? super E> clazz, String methodName, String methodNameObf, Class<?>... paramTypes)
    {
        Exception failed = null;

        try
        {
            Method method = ReflectionHelper.findMethod(clazz, null, new String[] { methodNameObf, methodName }, paramTypes);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflect(method);
            method.setAccessible(false);
            return handle;
        }
        catch (IllegalAccessException e)
        {
            failed = e;
        }

        throw new UnableToFindMethodHandleException(new String[] { methodNameObf, methodName }, failed);
    }
}
