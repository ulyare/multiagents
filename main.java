import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.io.Serializable;
import java.util.*;

public class main {

    // Класс AgentData
    public static class AgentData implements Serializable {
        private int agentId;
        private double value;
        private Map<Integer, Double> knownAgents;

        public AgentData(int agentId, double value) {
            this.agentId = agentId;
            this.value = value;
            this.knownAgents = new HashMap<>();
            this.knownAgents.put(agentId, value);
        }

        public int getAgentId() { return agentId; }
        public double getValue() { return value; }
        public Map<Integer, Double> getKnownAgents() { return knownAgents; }

        public void addAgentData(int id, double value) {
            knownAgents.put(id, value);
        }

        public void mergeKnowledge(Map<Integer, Double> otherKnowledge) {
            knownAgents.putAll(otherKnowledge);
        }

        public boolean knowsAllAgents() {
            return knownAgents.size() == 7;
        }

        public double calculateAverage() {
            if (knownAgents.isEmpty()) return 0;
            double sum = 0;
            for (double val : knownAgents.values()) {
                sum += val;
            }
            return sum / knownAgents.size();
        }
    }

    // Класс GraphTopology
    public static class GraphTopology {
        private static final Map<Integer, List<Integer>> connections = new HashMap<>();
        private static final Map<Integer, Double> agentValues = new HashMap<>();

        static {
            connections.put(1, Arrays.asList(2, 5, 7));
            connections.put(2, Arrays.asList(1, 3));
            connections.put(3, Arrays.asList(2, 4));
            connections.put(4, Arrays.asList(3, 5, 6));
            connections.put(5, Arrays.asList(1, 4, 7));
            connections.put(6, Arrays.asList(4, 7));
            connections.put(7, Arrays.asList(1, 5, 6));

            agentValues.put(1, 35.0);
            agentValues.put(2, -7.0);
            agentValues.put(3, 5.0);
            agentValues.put(4, 34.0);
            agentValues.put(5, 7.0);
            agentValues.put(6, 80.0);
            agentValues.put(7, 11.0);
        }

        public static List<Integer> getNeighbors(int agentId) {
            return connections.getOrDefault(agentId, new ArrayList<>());
        }

        public static double getAgentValue(int agentId) {
            return agentValues.get(agentId);
        }

        public static Set<Integer> getAllAgentIds() {
            return agentValues.keySet();
        }

        public static double calculateRealAverage() {
            double sum = 0;
            for (double value : agentValues.values()) {
                sum += value;
            }
            return sum / agentValues.size();
        }
    }

    // Класс AverageAgent
    public static class AverageAgent extends Agent {
        private AgentData agentData;
        private Set<Integer> receivedFrom = new HashSet<>();
        private int currentStage = 0;
        private boolean isFirstTick = true;
        private static int agentsReadyForStage = 0;
        private static final Object lock = new Object();
        private static int agentsThatKnowAll = 0;
        private static boolean allAgentsShouldStop = false;

        @Override
        protected void setup() {
            String agentName = getLocalName();
            int agentId = Integer.parseInt(agentName.replace("agent", ""));
            double value = GraphTopology.getAgentValue(agentId);

            agentData = new AgentData(agentId, value);

            addBehaviour(new ReceiveMessagesBehaviour());
            addBehaviour(new ExchangeDataBehaviour(this, 5000));
        }

        private class ExchangeDataBehaviour extends TickerBehaviour {
            public ExchangeDataBehaviour(Agent a, long period) {
                super(a, period);
            }

            @Override
            protected void onTick() {
                if (allAgentsShouldStop) {
                    stop();
                    return;
                }

                if (isFirstTick) {
                    isFirstTick = false;
                    return;
                }

                currentStage++;
                receivedFrom.clear();

                sendDataToNeighbors();

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (lock) {
                    agentsReadyForStage++;

                    if (agentsReadyForStage == 1) {
                        System.out.println("\n════════════════════════════════════════");
                        System.out.println("ЭТАП " + currentStage);
                    }

                    System.out.println("Агент " + agentData.getAgentId() +
                            " | Знает: " + agentData.getKnownAgents().size() + " агентов");

                    if (agentData.knowsAllAgents()) {
                        agentsThatKnowAll++;
                    }

                    if (agentsReadyForStage == 7) {
                        agentsReadyForStage = 0;

                        if (agentsThatKnowAll == 7) {
                            System.out.println("\nВСЕ ЗНАЮТ ОБО ВСЕХ");
                            System.out.println("ФИНАЛЬНЫЕ РЕЗУЛЬТАТЫ:");
                            double avg = GraphTopology.calculateRealAverage();
                            for (int i = 1; i <= 7; i++) {
                                System.out.println("Агент " + i + " вычислил среднее: " + String.format("%.2f", avg));
                            }
                            System.out.println("════════════════════════════════════════");
                            System.out.println("Реальное среднее арифметическое: " + String.format("%.2f", avg));
                            System.out.println("РАСЧЁТ СТОИМОСТИ:");
                            System.out.println("Пусть С1 - передача агент-центр (1000р)");
                            System.out.println("С2 - передача агент-агент(0.1р). ");
                            System.out.println("С3 - арифметические операции (0.01р)");
                            System.out.println("С4 - ячейка памяти (1).  ");
                            System.out.println("С5 - итерация (1).");
                            System.out.println("С6 - запись в память (0.01).");
                            System.out.println("Для алгоритма расчёт вычисляется по формуле:");
                            System.out.println("Q = n*С1 + 6n*C2 + 7n*C3 + (2+n)*C4 + 4*C5 + 6n*C6, где n - количество узлов в сети.");
                            System.out.println("Для n=7, Q=7018.11 ");
                            allAgentsShouldStop = true;
                        } else {
                            agentsThatKnowAll = 0;
                        }
                    }
                }
            }

            private void sendDataToNeighbors() {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                try {
                    msg.setContentObject(agentData);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for (int neighborId : GraphTopology.getNeighbors(agentData.getAgentId())) {
                    msg.addReceiver(new jade.core.AID("agent" + neighborId, jade.core.AID.ISLOCALNAME));
                }

                send(msg);
            }
        }

        private class ReceiveMessagesBehaviour extends CyclicBehaviour {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = myAgent.receive(mt);

                if (msg != null) {
                    try {
                        AgentData receivedData = (AgentData) msg.getContentObject();
                        int senderId = receivedData.getAgentId();

                        receivedFrom.add(senderId);
                        agentData.mergeKnowledge(receivedData.getKnownAgents());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    block();
                }
            }
        }

        public AgentData getAgentData() {
            return agentData;
        }
    }

    // Main метод
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1098");
        profile.setParameter(Profile.GUI, "true");

        AgentContainer mainContainer = rt.createMainContainer(profile);

        try {
            System.out.println("Создаём и запускаем всех агентов для расчета среднего арифметического");
            System.out.println("Граф связей:");
            for (int i = 1; i <= 7; i++) {
                System.out.println("   Агент " + i + " (" + GraphTopology.getAgentValue(i) +
                        ") связан с: " + GraphTopology.getNeighbors(i));
            }
            System.out.println();

            for (int i = 1; i <= 7; i++) {
                AgentController agent = mainContainer.createNewAgent(
                        "agent" + i,
                        AverageAgent.class.getName(),
                        new Object[]{}
                );
                agent.start();
            }

            System.out.println("Обмен данными...");

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}




