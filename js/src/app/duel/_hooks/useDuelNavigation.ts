import { useMyDuelOrPartyData } from "@/lib/api/queries/duels/sse";
import { useEffect } from "react";
import { useNavigate } from "react-router";

export function useLobbyNavigation() {
  const navigate = useNavigate();
  const { data } = useMyDuelOrPartyData();

  useEffect(() => {
    if (data?.success) {
      navigate("/duel/current");
    } else {
      navigate("/duel");
    }
  }, [data?.success, navigate]);

  return;
}
