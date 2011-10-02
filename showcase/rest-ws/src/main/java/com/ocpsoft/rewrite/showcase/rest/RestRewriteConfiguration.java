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
package com.ocpsoft.rewrite.showcase.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import com.ocpsoft.rewrite.bind.Evaluation;
import com.ocpsoft.rewrite.bind.ParameterizedPattern;
import com.ocpsoft.rewrite.config.Configuration;
import com.ocpsoft.rewrite.config.ConfigurationBuilder;
import com.ocpsoft.rewrite.context.EvaluationContext;
import com.ocpsoft.rewrite.servlet.config.HttpConfigurationProvider;
import com.ocpsoft.rewrite.servlet.config.HttpOperation;
import com.ocpsoft.rewrite.servlet.config.Method;
import com.ocpsoft.rewrite.servlet.config.Path;
import com.ocpsoft.rewrite.servlet.config.Response;
import com.ocpsoft.rewrite.servlet.http.event.HttpInboundServletRewrite;
import com.ocpsoft.rewrite.servlet.http.event.HttpServletRewrite;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class RestRewriteConfiguration extends HttpConfigurationProvider
{
   @Inject
   private ProductRegistry products;

   @Override
   public Configuration getConfiguration(final ServletContext context)
   {
      return ConfigurationBuilder
               .begin()

               .defineRule()

               /**
                * Define the inbound conditions and conversion mechanisms to be used when handling inbound requests.
                */
               .when(Method.isGet()
                        .and(Path.matches("/store/product/{pid}")
                                 .where("pid")
                                 .bindsTo(Evaluation.property("pid").convertedBy(ProductConverter.class)
                                          .validatedBy(ProductValidator.class))))
               .perform(new HttpOperation() {
                  @Override
                  public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
                  {
                     /**
                      * Extract the stored {pid} from our Path and load the Product. This is an example of how we can
                      * use a converter to directly bind and store the object we want into a binding. {@link Evaluation}
                      * is an low-level construct, and binds array values that must be dereferenced. If using other
                      * bindings such as {@link El}, the value will be bound directly to the type of the referenced
                      * property type, and this array downcast is not necessary.
                      */
                     Product product = (Product) Evaluation.property("pid").retrieve(event, context);

                     /**
                      * Marshal the Product into XML using JAXB. This has been extracted into a utility class.
                      */
                     try {
                        XMLUtil.streamFromObject(Product.class, product, event.getResponse()
                                 .getOutputStream());
                     }
                     catch (IOException e) {
                        throw new RuntimeException(e);
                     }

                     /**
                      * Set the content type and status code of the response, this again could be extracted into a REST
                      * utility class.
                      */
                     event.getResponse().setContentType("application/xml");
                     ((HttpInboundServletRewrite) event).sendStatusCode(200);
                  }
               })

               .defineRule()
               .when(Path.matches("/store/products").and(Method.isGet()))
               .perform(new HttpOperation() {
                  @Override
                  public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
                  {
                     try {
                        XMLUtil.streamFromObject(ProductRegistry.class, products, event.getResponse().getOutputStream());
                        event.getResponse().setContentType("application/xml");
                        ((HttpInboundServletRewrite) event).sendStatusCode(200);
                     }
                     catch (Exception e) {
                        throw new RuntimeException(e);
                     }
                  }
               })

               .defineRule()
               .when(Path.matches("/store/products").and(Method.isPost()))
               .perform(new HttpOperation() {
                  @Override
                  public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
                  {
                     try {
                        Product product = XMLUtil.streamToObject(Product.class, event.getRequest().getInputStream());
                        product = products.add(product);

                        /**
                         * Just for fun, set a response header containing the URL to the newly created Product.
                         */
                        String location = new ParameterizedPattern(event.getContextPath() + "/store/product/{pid}")
                                 .build(product.getId());
                        Response.addHeader("Location", location).perform(event, context);

                        event.getResponse().setContentType("text/html");
                        ((HttpInboundServletRewrite) event).sendStatusCode(200);
                     }
                     catch (Exception e) {
                        throw new RuntimeException(e);
                     }
                  }
               });
   }

   @Override
   public int priority()
   {
      return 0;
   }

}
