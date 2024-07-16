package br.com.alura.screenmatch_v300.principal;

import br.com.alura.screenmatch_v300.models.*;
import br.com.alura.screenmatch_v300.repository.SerieRepository;
import br.com.alura.screenmatch_v300.service.ConsumoApi;
import br.com.alura.screenmatch_v300.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private List<DadosSerie> dadosSeries = new ArrayList<>();

    private SerieRepository repository;
    private List<Serie> series = new ArrayList<>();

    private Optional<Serie> serieBuscada;

    public Principal(SerieRepository repository){
        this.repository = repository;
    }

    public void exibeMenu() {

        var opcao = -1;
        while (opcao != 0){
            var menu = """
                1 - Buscar séries
                2 - Buscar episódios
                3 - Listar Séries buscadas
                4 - Buscar série por titulo
                5 - Buscar série por ator
                6 - Top 5 séries
                7 - Buscar séries por categoria
                8 - Buscar séries por avaliação e temporadas
                9 - Buscar nome do episódio
                10 - Top 5 Episódios por série
                11- Buscar episódios a partir de uma data
                
                0 - Sair                                 
                """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();



            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    buscarSeriesPorTemporadaEAvaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    TopEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosPrData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }


    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        //dadosSeries.add(dados);
        repository.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        //DadosSerie dadosSerie = getDadosSerie();
        listarSeriesBuscadas();
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repository.findByTituloContainingIgnoreCase(nomeSerie);
        if (serie.isPresent()){
            var serieEncontrada = serie.get();
            List<DadosTemporadas> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporadas dadosTemporada = conversor.obterDados(json, DadosTemporadas.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repository.save(serieEncontrada);
        }else {
            System.out.println("Série não encontrada!");
        }
    }

    private void listarSeriesBuscadas(){
//        List<Serie> series = new ArrayList<>();
//        series = dadosSeries.stream()
//                .map(d -> new Serie(d))
//                        .collect(Collectors.toList());
        series = repository.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }


    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        serieBuscada = repository.findByTituloContainingIgnoreCase(nomeSerie);
        if (serieBuscada.isPresent()){
            System.out.println("Dados da série: " + serieBuscada.get());
        }else {
            System.out.println("Série não encontrada!");
        }
    }

    private void buscarSeriePorAtor() {
        System.out.println("Digite o nome do ator para busca: ");
        var nomeAtor = leitura.nextLine();
        System.out.println("Séries com nota a partir de que valor?");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();

        List<Serie> seriesEncontradas = repository.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        if (nomeAtor.isEmpty()){
            System.out.println("Ator não encontrado!");
        }else {
            System.out.println("Series em que " + nomeAtor + "trabalhou:");
            seriesEncontradas.forEach(s ->
                            System.out.println( s.getTitulo() + " - avaliação: " + s.getAvaliacao()));
        }


    }

    private void buscarTop5Series() {
        List<Serie> seriesTop = repository.findTop5ByOrderByAvaliacaoDesc();
        seriesTop.forEach(s ->
                System.out.println( s.getTitulo() + " - avaliação: " + s.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria() {
        System.out.println("Deseja buscar séries de que categoria/gênero? ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromStringPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repository.findByGenero(categoria);
        System.out.println("Séries da categoria " + nomeGenero);
        seriesPorCategoria.forEach(System.out::println);
    }


    private void buscarSeriesPorTemporadaEAvaliacao() {
        System.out.println("Informe qual o número máximo de temporadas que deseja encontrar: ");
        var numeroTemporadas = leitura.nextInt();
        leitura.nextLine();
        System.out.println("Informe um valor mínimo de avaliações desejadas: ");
        var avaliacaoMinima = leitura.nextDouble();
        leitura.nextLine();

        List<Serie> buscaPersonalizada = repository.seriesPorTemporadaEAvaliacao(numeroTemporadas, avaliacaoMinima);
        System.out.println("Séries com uma temporada menor ou igual a " + numeroTemporadas + " e avaliação mínima de " + avaliacaoMinima);
        buscaPersonalizada.forEach(System.out::println);
//      buscaPersonalizada.forEach(s ->
//                System.out.println(s.getTitulo() + "  - avaliação: " + s.getAvaliacao()));
    }


    private void buscarEpisodioPorTrecho() {
        System.out.println("Qual nome do episódio para busca? ");
        var trechoEpisodio = leitura.nextLine();

        List<Episodio> episodiosEncontrados = repository.episodiosPorTrecho(trechoEpisodio);
        episodiosEncontrados.forEach(e ->
                System.out.printf("Série: %s Temporada %s - Episódio %s - %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(),
                        e.getNumero(), e.getTitulo()));
    }

    private void TopEpisodiosPorSerie() {
        buscarSeriePorTitulo();
        if(serieBuscada.isPresent()){
          Serie serie = serieBuscada.get();
          List<Episodio> topEpisodios = repository.topEpisodiosPorSerie(serie);
          topEpisodios.forEach(e ->
                  System.out.printf("Série: %s Temporada %s - Episódio %s - %s - Avaliação %s\n",
                          e.getSerie().getTitulo(), e.getTemporada(),
                          e.getNumero(), e.getTitulo(), e.getAvaliacao()));
        }
    }


    private void buscarEpisodiosPrData() {
        buscarSeriePorTitulo();
        if (serieBuscada.isPresent()){
            Serie serie = serieBuscada.get();
            System.out.println("Digite o an limite de lançamento: ");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodiosAno = repository.episodiosPorSerieEAno(serie, anoLancamento);
            episodiosAno.forEach(System.out::println);
        }
    }



}
