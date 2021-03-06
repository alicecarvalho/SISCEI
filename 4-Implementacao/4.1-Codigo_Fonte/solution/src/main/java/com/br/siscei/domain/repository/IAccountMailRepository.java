package com.br.siscei.domain.repository;
import java.util.concurrent.Future;

import com.br.siscei.domain.entity.account.User;
 
/**
 * Interface para o envio de e-mails
 *
 * @author rodrigo.p.fraga@gmail.com
 * @since 02/10/2014
 * @version 1.0
 */
public interface IAccountMailRepository
{
    /*-------------------------------------------------------------------
     *                          BEHAVIORS
     *-------------------------------------------------------------------*/
    /**
     * @param user
     */
    public Future<Void> sendNewUserAccount( User user );
}