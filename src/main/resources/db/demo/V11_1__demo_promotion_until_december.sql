-- Perfil demo: a promocao de Acessorios da V6_2 passa a valer ate o fim de dezembro/2026, para cobrir a
-- apresentacao (29/11) e os ensaios. So altera a linha original do seed; um banco onde ela foi encerrada
-- ou editada pela tela de Promocoes fica como esta. O autor (created_by) continua o gerente da V6_1.
UPDATE promotion
   SET name = 'Acessorios 10% - outubro a dezembro',
       ends_at = '2027-01-01 00:00-03'
 WHERE name = 'Acessorios 10% - outubro'
   AND ends_at = '2026-11-01 00:00-03';
