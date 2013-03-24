/*
 * Copyright 2011 <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ocpsoft.rewrite.servlet.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.ocpsoft.common.util.Streams;
import org.ocpsoft.rewrite.config.Operation;
import org.ocpsoft.rewrite.context.EvaluationContext;
import org.ocpsoft.rewrite.exception.RewriteException;
import org.ocpsoft.rewrite.servlet.RewriteWrappedResponse;
import org.ocpsoft.rewrite.servlet.config.response.ResponseContentInterceptor;
import org.ocpsoft.rewrite.servlet.config.response.ResponseStreamWrapper;
import org.ocpsoft.rewrite.servlet.http.event.HttpServletRewrite;

/**
 * Responsible for manipulating properties such as headers, cookies, {@link ResponseContentInterceptor} and
 * {@link ResponseStreamWrapper} instances to the curent {@link HttpServletResponse}
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public abstract class Response extends HttpOperation
{
   /**
    * Create an {@link Operation} that adds the given {@link ResponseContentInterceptor} instances to the current
    * {@link ServletResponse}. This will activate response buffering on the current {@link ServletRequest} - meaning
    * that generated output will not be sent to the client until the entire request has completed and all registered
    * {@link ResponseContentInterceptor} instances have been executed on the outbound response content.
    * 
    * <p>
    * <b>WARNING:</b> This will cause the <b>ENTIRE</b> response to be buffered in memory, which may cause performance
    * issues on larger responses. Make sure you you really need to buffer the entire response! Favor using a
    * {@link ResponseStreamWrapper} if desired behavior may be performed as a stream operation; this will result in far
    * less memory overhead.
    * 
    * @throws IllegalStateException When output has already been written to the client.
    */
   public static Response withOutputInterceptedBy(final ResponseContentInterceptor... buffers)
            throws IllegalStateException
   {
      return new Response() {
         @Override
         public void performHttp(HttpServletRewrite event, EvaluationContext context)
         {
            for (ResponseContentInterceptor buffer : buffers) {
               RewriteWrappedResponse.getCurrentInstance(event.getRequest()).addContentInterceptor(buffer);
            }
         }

         @Override
         public String toString()
         {
            return "Response.withOutputInterceptedBy(" + buffers + ")";
         }
      };
   }

   /**
    * Create an {@link Operation} that adds the given {@link ResponseStreamWrapper} instances to the current
    * {@link ServletResponse}. This will activate response stream wrapping on the current {@link ServletRequest} -
    * meaning response content will be piped through all registered {@link ResponseStreamWrapper} instances as it is
    * written to the client {@link ServletOutputStream}.
    * 
    * @throws IllegalStateException When output has already been written to the client.
    */
   public static Response withOutputStreamWrappedBy(final ResponseStreamWrapper... wrappers)
            throws IllegalStateException
   {
      return new Response() {
         @Override
         public void performHttp(HttpServletRewrite event, EvaluationContext context)
         {
            for (ResponseStreamWrapper wrapper : wrappers) {
               RewriteWrappedResponse.getCurrentInstance(event.getRequest()).addStreamWrapper(wrapper);
            }
         }

         @Override
         public String toString()
         {
            return "Response.withOutputStreamWrappedBy(" + wrappers + ")";
         }
      };
   }

   /**
    * Create an {@link Operation} that adds a header to the {@link HttpServletResponse}
    */
   public static Response addHeader(final String name, final String value)
   {
      return new Response() {
         @Override
         public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
         {
            event.getResponse().addHeader(name, value);
         }

         @Override
         public String toString()
         {
            return "Response.addHeader(\"" + name + "\", " + value + ")";
         }
      };
   }

   /**
    * Create an {@link Operation} that adds a date header to the {@link HttpServletResponse}
    */
   public static Response addDateHeader(final String name, final long value)
   {
      return new Response() {
         @Override
         public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
         {
            event.getResponse().addDateHeader(name, value);
         }

         @Override
         public String toString()
         {
            return "Response.addDateHeader(\"" + name + "\", " + value + ")";
         }
      };
   }

   /**
    * Create an {@link Operation} that adds an <code>int</code> header to the {@link HttpServletResponse}
    */
   public static Response addIntHeader(final String name, final int value)
   {
      return new Response() {
         @Override
         public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
         {
            event.getResponse().addIntHeader(name, value);
         }

         @Override
         public String toString()
         {
            return "Response.addIntHeader(\"" + name + "\", " + value + ")";
         }
      };
   }

   /**
    * Create an {@link Operation} that adds a {@link Cookie} to the {@link HttpServletResponse}
    */
   public static Response addCookie(final Cookie cookie)
   {
      return new Response() {
         @Override
         public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
         {
            event.getResponse().addCookie(cookie);
         }

         @Override
         public String toString()
         {
            return "Response.addCookie(" + cookie + ")";
         }
      };
   }

   /**
    * Create an {@link Operation} that sets the {@link HttpServletResponse#setStatus(int)} code of the current
    * {@link HttpServletResponse}.
    */
   public static Response setCode(final int code)
   {
      return new Response() {
         @Override
         public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
         {
            event.getResponse().setStatus(code);
         }

         @Override
         public String toString()
         {
            return "Response.setCode(" + code + ")";
         }
      };
   }

   /**
    * Create an {@link Operation} that writes the given bytes to the current {@link HttpServletResponse} upon execution.
    */
   public static Response write(final byte... bytes)
   {
      return write(new ByteArrayInputStream(bytes));
   }

   /**
    * Create an {@link Operation} that writes the {@link String} value of the given {@link Object} to the current
    * {@link HttpServletResponse} upon execution. The value to be written is obtained using {@link Object#toString()}.
    */
   public static Response write(final Object value)
   {
      return new Response() {
         @Override
         public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
         {
            try {
               if (value != null)
                  event.getResponse().getWriter().write(value.toString());
            }
            catch (IOException e) {
               throw new RewriteException("Could not write value [" + value + "] to response stream.", e);
            }
         }

         @Override
         public String toString()
         {
            return "Response.write(" + value + ")";
         }
      };
   }

   /**
    * Create an {@link Operation} that writes the contents of the given {@link InputStream} current
    * {@link HttpServletResponse} upon execution.
    */
   public static Response write(final InputStream stream)
   {
      return new Response() {
         @Override
         public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
         {
            try {
               Streams.copy(stream, event.getResponse().getOutputStream());
            }
            catch (IOException e) {
               throw new RewriteException("Could not write stream [" + stream + "] to response stream.", e);
            }
         }

         @Override
         public String toString()
         {
            return "Response.write(" + stream + ")";
         }
      };
   }

}
